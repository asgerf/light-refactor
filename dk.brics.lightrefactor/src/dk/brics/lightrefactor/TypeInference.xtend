package dk.brics.lightrefactor

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Map
import java.util.Set
import org.mozilla.javascript.Token
import org.mozilla.javascript.ast.ArrayComprehension
import org.mozilla.javascript.ast.ArrayLiteral
import org.mozilla.javascript.ast.Assignment
import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.AstRoot
import org.mozilla.javascript.ast.Block
import org.mozilla.javascript.ast.BreakStatement
import org.mozilla.javascript.ast.ConditionalExpression
import org.mozilla.javascript.ast.ContinueStatement
import org.mozilla.javascript.ast.DoLoop
import org.mozilla.javascript.ast.ElementGet
import org.mozilla.javascript.ast.EmptyExpression
import org.mozilla.javascript.ast.EmptyStatement
import org.mozilla.javascript.ast.ErrorNode
import org.mozilla.javascript.ast.ExpressionStatement
import org.mozilla.javascript.ast.ForInLoop
import org.mozilla.javascript.ast.ForLoop
import org.mozilla.javascript.ast.FunctionCall
import org.mozilla.javascript.ast.FunctionNode
import org.mozilla.javascript.ast.GeneratorExpression
import org.mozilla.javascript.ast.IfStatement
import org.mozilla.javascript.ast.InfixExpression
import org.mozilla.javascript.ast.KeywordLiteral
import org.mozilla.javascript.ast.LabeledStatement
import org.mozilla.javascript.ast.Name
import org.mozilla.javascript.ast.NewExpression
import org.mozilla.javascript.ast.NumberLiteral
import org.mozilla.javascript.ast.ObjectLiteral
import org.mozilla.javascript.ast.ParenthesizedExpression
import org.mozilla.javascript.ast.PropertyGet
import org.mozilla.javascript.ast.RegExpLiteral
import org.mozilla.javascript.ast.ReturnStatement
import org.mozilla.javascript.ast.Scope
import org.mozilla.javascript.ast.StringLiteral
import org.mozilla.javascript.ast.SwitchStatement
import org.mozilla.javascript.ast.ThrowStatement
import org.mozilla.javascript.ast.TryStatement
import org.mozilla.javascript.ast.UnaryExpression
import org.mozilla.javascript.ast.VariableDeclaration
import org.mozilla.javascript.ast.WhileLoop
import org.mozilla.javascript.ast.WithStatement

import static extension dk.brics.lightrefactor.NameRef.*
import static extension dk.brics.lightrefactor.util.MapExtensions.*

class TypeInference {
  
  new (AstRoot ast) {
    this(Collections::singletonList(ast))
  }
  new (Iterable<AstRoot> asts) {
    for (ast : asts) {
      visitAST(ast)
    }
    nativeModel()
    receiverTypes()
    subtyping(asts)
  }
  
  private val unifier = new TypeUnifier
  private val global = new TypeNode
  
  private val nodemap = new HashMap<AstNode, TypeNode>
  private val scopemap = new HashMap<Scope, TypeNode>
  
  private val potentialMethods = new ArrayList<TypeNode> // occurs in pairs of two: host followed by receiver
  
  private static val VOID = true
  private static val NOT_VOID = false
  
  private static val PRIMITIVE = true
  private static val NOT_PRIMITIVE = false
  
  private def scopeNode(Scope x) {
    var n = scopemap.get(x)
    if (n == null) {
      n = new TypeNode
      scopemap.put(x, n)
    }
    return n
  }
  
  private def typeNode(AstNode x) {
    var n = nodemap.get(x)
    if (n == null) {
      n = new TypeNode
      nodemap.put(x, n)
    }
    return n
  }
  
  private def unify(TypeNode x, TypeNode ... ys) {
    for (y : ys) {
      unifier.unify(x, y)
    }
  }
  
  private def unifyPrty(TypeNode x, String name, TypeNode y) {
    unifier.unifyPrty(x, name, y)
  }
  
  private def addFunction(TypeNode node, FunctionNode fun) {
    if (node.functions == null) {
      node.functions = new LinkNode<FunctionNode>(fun)
    } else {
      node.functions.splice(new LinkNode<FunctionNode>(fun))
    }
  }
  
  private def unfold(AstNode exp) {
    var e = exp
    while (e instanceof ParenthesizedExpression) {
      e = (e as ParenthesizedExpression).expression
    }
    return e
  }
  
  private def varNode(FunctionNode node, String name) {
    val scope = node.scopeNode
    var x = scope.prty.get(name)
    if (x == null) {
      x = new TypeNode
      scope.prty.put(name, x)
    }
    return x
  }
  
  private def markNamespace(AstNode node) {
    node.typeNode.rep().namespace = true
  }
  
  private def markConstructor(AstNode node) {
    switch node {
      PropertyGet:
        markNamespace(node.left)
    }
  }
  
  private def addPotentialMethod(TypeNode host, TypeNode receiver) {
    potentialMethods.add(host)
    potentialMethods.add(receiver)
  }
  
  
  def boolean visitExp(AstNode exp, boolean voidctx) {
    switch exp {
      ArrayLiteral: {
          for (AstNode elem : exp.elements) {
            visitExp(elem, NOT_VOID)
            unifyPrty(exp.typeNode, "@array", elem.typeNode)
          }
          return NOT_PRIMITIVE
       }
      ConditionalExpression: {
          visitExp(exp.testExpression, VOID)
          val p1 = visitExp(exp.trueExpression, voidctx)
          val p2 = visitExp(exp.falseExpression, voidctx)
          if (!voidctx) {
            unify(exp.trueExpression.typeNode, exp.falseExpression.typeNode)
          }
          return p1 && p2
        }
      ElementGet: {
          visitExp(exp.target, NOT_VOID)
          visitExp(exp.element, NOT_VOID)
          if (exp.element instanceof StringLiteral) { // TODO: ignore strings that look like numbers
            unifyPrty(exp.target.typeNode, (exp.element as StringLiteral).value, exp.typeNode)
          }
          return NOT_PRIMITIVE
        }
      EmptyExpression:
        return PRIMITIVE
      FunctionCall: {
        visitExp(exp.target, NOT_VOID)
        exp.arguments.forEach [ visitExp(it, NOT_VOID) ]
        val target = unfold(exp.target)
        switch target {
          FunctionNode: {
            val numArgs = Math::min(exp.arguments.size, target.paramCount)
            for (i : 0..<numArgs) {
              unify(exp.arguments.get(i).typeNode, target.params.get(i).typeNode)
            }
            unifyPrty(target.scopeNode, "@return", exp.typeNode)
            if (exp instanceof NewExpression) {
              unifyPrty(target.scopeNode, "@this", exp.typeNode)
            } else {
              unifyPrty(target.scopeNode, "@this", global)
            }
          }
        }
        if (exp instanceof NewExpression) {
          markConstructor(target)
        }
        return NOT_PRIMITIVE
      }
      Assignment: {
        visitExp(exp.left, NOT_VOID)
        val prim = visitExp(exp.right, NOT_VOID)
        if (!prim && exp.type == Token::ASSIGN) {
          unify(exp.typeNode, exp.left.typeNode, exp.right.typeNode)
          
          // check for method definition: e.f = function() {..}
          if (exp.left instanceof PropertyGet && exp.right instanceof FunctionNode) {
            val prtyget = exp.left as PropertyGet
            val fun = exp.right as FunctionNode
            addPotentialMethod(prtyget.left.typeNode, fun.varNode("@this"))
          }
          
          return NOT_PRIMITIVE
        } else {
          return PRIMITIVE
        }
      }
      PropertyGet: {
        visitExp(exp.left, NOT_VOID) // note: do not visit right, because it is not an expression
        unifyPrty(exp.left.typeNode, exp.right.string, exp.typeNode)
        // if `e.prototype`, then mark `e` as a constructor
        if (exp.right.string == "prototype") {
          markConstructor(exp.left)
        }
        return NOT_PRIMITIVE
      }
      InfixExpression case exp.type == Token::AND: { // && operator
        visitExp(exp.left, VOID)
        val prim = visitExp(exp.right, voidctx)
        unify(exp.typeNode, exp.right.typeNode)
        return prim
      }
      InfixExpression case exp.type == Token::OR: { // || operator
        val p1 = visitExp(exp.left, voidctx)
        val p2 = visitExp(exp.right, voidctx)
        if (!voidctx) {
          unify(exp.typeNode, exp.left.typeNode, exp.right.typeNode)
        }
        return p1 && p2
      }
      InfixExpression: { // all other binary operators (note: must come after Assignment and PropertyGet)
        visitExp(exp.left, VOID)
        visitExp(exp.right, VOID)
        return PRIMITIVE
      } 
      ArrayComprehension:
        throw new RuntimeException("JavaScript 1.7+ feature not supported: array comprehension")
      GeneratorExpression:
        throw new RuntimeException("JavaScript 1.7+ feature not supported: generator expression")
      FunctionNode: {
        addFunction(exp.typeNode, exp)
        visitStmt(exp.body)
        val thisNode = new TypeNode
        unifyPrty(exp.typeNode, "prototype", thisNode)
        unifyPrty(exp.scopeNode, "@this", thisNode)
        if (!exp.name.equals("")) {
          unifyPrty(exp.scopeNode, exp.name, exp.typeNode)
        }
        return NOT_PRIMITIVE
      }
      KeywordLiteral case exp.type == Token::THIS: {
        val fun = exp.enclosingFunction
        if (fun != null) {
          unifyPrty(fun.scopeNode, "@this", exp.typeNode)
        } else {
          unify(global, exp.typeNode) // this at top-level is the global object
        }
        return NOT_PRIMITIVE
      }
      KeywordLiteral: // true, false, null
        return PRIMITIVE
      Name: {
        if (exp.string == "undefined") {
          return PRIMITIVE
        } else {
          val scope = exp.definingScope
          if (NameRef::isGlobalScope(scope)) {
            unifyPrty(global, exp.string, exp.typeNode)
          } else {
            unifyPrty(scope.scopeNode, exp.string, exp.typeNode)
          }
          return NOT_PRIMITIVE
        }
      }
      NumberLiteral:
        return PRIMITIVE
      ObjectLiteral: {
        val obj = exp.typeNode
        for (prop : exp.elements) {
          visitExp(prop.right, NOT_VOID)
          val lnode = prop.left
          val name = switch lnode {
            Name: lnode.string
            StringLiteral: lnode.value
            NumberLiteral: null
          }
          if (name != null) {
            if (prop.getter) {
              val fun = prop.right as FunctionNode
              unifyPrty(fun.scopeNode, "@this", obj)
              val ret = new TypeNode
              unifyPrty(obj, name, ret)
              unifyPrty(fun.scopeNode, "@return", ret)
            } else if (prop.setter) {
              val fun = prop.right as FunctionNode
              unifyPrty(fun.scopeNode, "@this", obj)
              if (fun.paramCount >= 1) {
                val t = new TypeNode
                unifyPrty(obj, name, t)
                unifyPrty(fun.scopeNode, fun.params.get(0).string, t)
              }
            } else { // initializer
              unifyPrty(obj, name, prop.right.typeNode)
              
              // check for method definition: { f: function() {..} }
              if (prop.right instanceof FunctionNode) {
                val fun = prop.right as FunctionNode
                addPotentialMethod(obj, fun.varNode("@this"))
              }
            }
          }
        }
        return NOT_PRIMITIVE
      }
      ParenthesizedExpression:
        return visitExp(exp.expression, voidctx)
      RegExpLiteral:
        return NOT_PRIMITIVE // ??
      StringLiteral:
        return PRIMITIVE
      UnaryExpression: {
        visitExp(exp.operand, VOID)
        return PRIMITIVE
      }
      ErrorNode: // in case of a syntax error
        return PRIMITIVE 
      default:
        throw new RuntimeException("Unrecognized expression: " + exp.getClass)
    }
  }
  
  def void visitStmt(AstNode stmt) {
    switch stmt {
      Block: {
        for (child : stmt) {
          visitStmt(child as AstNode)
        }
      }
      EmptyStatement: {
      }
      ExpressionStatement: {
        visitExp(stmt.expression, VOID)
      }
      IfStatement: {
        visitExp(stmt.condition, VOID)
        visitStmt(stmt.thenPart)
        if (stmt.elsePart != null) {
          visitStmt(stmt.elsePart)
        }
      }
      BreakStatement: {
      }
      ContinueStatement: {
      }
      DoLoop: {
        visitStmt(stmt.body)
        visitExp(stmt.condition, VOID)
      }
      ForInLoop: {
        if (stmt.getIterator() instanceof VariableDeclaration) { // note: iterator() and getIterator() are different functions!
          visitStmt(stmt.getIterator())
        } else {
          visitExp(stmt.getIterator(), VOID) 
        }
        visitExp(stmt.iteratedObject, NOT_VOID)
        visitStmt(stmt.body)
      }
      ForLoop: {
        if (stmt.initializer != null) {
          if (stmt.initializer instanceof VariableDeclaration) {
            visitStmt(stmt.initializer)
          } else {
            visitExp(stmt.initializer, VOID)
          }
        }
        if (stmt.condition != null) {
          visitExp(stmt.condition, VOID)
        }
        if (stmt.increment != null) {
          visitExp(stmt.increment, VOID)
        }
        visitStmt(stmt.body)
      }
      WhileLoop: {
        visitExp(stmt.condition, VOID)
        visitStmt(stmt.body)
      }
      FunctionNode: {
        addFunction(stmt.typeNode, stmt)
        val scope = stmt.enclosingFunction
        if (scope == null) { // top-level function declaration
          unifyPrty(global, stmt.name, stmt.typeNode)
        } else {
          unifyPrty(scope.scopeNode, stmt.name, stmt.typeNode)
        }
        val thisNode = new TypeNode
        unifyPrty(stmt.typeNode, "prototype", thisNode)
        unifyPrty(stmt.scopeNode, "@this", thisNode)
        visitStmt(stmt.body)
      }
      SwitchStatement: {
        val prim = visitExp(stmt.expression, NOT_VOID)
        for (clause : stmt.cases) {
          if (clause.expression != null) {
            visitExp(clause.expression, if (prim) VOID else NOT_VOID)
          }
          if (clause.statements != null) { // it may be null instead of empty (wtf rhino?)
            for (child : clause.statements) {
              visitStmt(child)
            }
          }
        }
      }
      KeywordLiteral case stmt.type == Token::DEBUGGER: {
      }
      LabeledStatement: {
        visitStmt(stmt.statement)
      }
      ReturnStatement: {
        if (stmt.returnValue != null) {
          visitExp(stmt.returnValue, NOT_VOID)
          val fun = stmt.enclosingFunction
          if (fun != null) { // ignore returns at top-level
            unifyPrty(fun.scopeNode, "@return", stmt.returnValue.typeNode)
          }
        }
      }
      ThrowStatement: {
        visitExp(stmt.expression, VOID)
      }
      TryStatement: {
        visitStmt(stmt.tryBlock)
        for (catchClause : stmt.catchClauses) {
          visitStmt(catchClause.body)
        }
        if (stmt.finallyBlock != null) {
          visitStmt(stmt.finallyBlock)
        }
      }
      VariableDeclaration: {
        for (vinit : stmt.variables) {
          if (vinit.initializer != null) {
            visitExp(vinit.initializer, NOT_VOID)
            val scope = (vinit.target as Name).definingScope
            if (scope.isGlobalScope) {
              unifyPrty(global, vinit.target.string, vinit.initializer.typeNode)
            } else {
              unifyPrty(scope.scopeNode, vinit.target.string, vinit.initializer.typeNode)
            }
          }
        }
      }
      WithStatement: {
        visitExp(stmt.expression, NOT_VOID)
        visitStmt(stmt.statement)
      }
      Scope: {
        for (node : stmt) {
          visitStmt(node as AstNode)
        }
      }
      ErrorNode: {}
      default: {
        throw new RuntimeException("Unrecognized statement: " + stmt.getClass)
      }
    }
  }
  
  private def visitAST(AstRoot root) {
    for (stmt : root.statements) {
      visitStmt(stmt)
    }
  }
  
  private def receiverTypes() {
    unifier.complete()
    var i=0
    while (i < potentialMethods.size) {
      val x = potentialMethods.get(i)
      val y = potentialMethods.get(i+1)
      if (!x.namespace && !y.namespace) {
        unifier.unifyLater(x,y)
      }
      i = i + 2
    }
    potentialMethods.clear()
    unifier.complete()
  }
  
  private def void nativeModel() {
    // unify window with global object [TODO: externalize native model]
    unifier.unifyPrty(global, "window", global)
  }
  
  private val superTypes = new HashMap<TypeNode,HashSet<TypeNode>>
  private var Map<TypeNode, HashSet<TypeNode>> subTypes
  
  private def TypeNode getPrty(TypeNode node, String name) {
    var t = node.prty.get(name)
    if (t != null)
      return t
    t = new TypeNode
    node.prty.put(name, t)
    t
  }
  private def void addSubtyping(TypeNode concrete, TypeNode abstr) {
    superTypes.getSet(concrete).add(abstr)
  }
  
  def subtyping(Iterable<AstRoot> asts) {
    for (ast : asts) {
      ast.visit [ node |
        switch node {
          FunctionCall case node.target.typ.hasUniqueFunction: {
            val callee = node.target.typ.uniqueFunction
            addSubtyping(node.typ, callee.scopeNode.getPrty("@return"))
            val numArgs = Math::min(node.arguments.size, callee.paramCount)
            for (i : 0..<numArgs) {
              addSubtyping(node.arguments.get(i).typ, callee.scopeNode.getPrty(callee.params.get(i).string))
            }
          }
        }
        switch node {
          NewExpression: {
            addSubtyping(node.typ, node.target.typ.getPrty("prototype"))
          }
        }
        true
      ]
    }
    subTypes = superTypes.inverse
    // TODO: collapse cycles??
  }
  
  def Set<TypeNode> subTypes(TypeNode node) {
    subTypes.trySet(node)
  }
  def Set<TypeNode> superTypes(TypeNode node) {
    superTypes.trySet(node)
  }
  
  def TypeNode typ(AstNode node) {
    return node.typeNode.rep()
  } 
  
  def getGlobal() { global.rep() }
  
}