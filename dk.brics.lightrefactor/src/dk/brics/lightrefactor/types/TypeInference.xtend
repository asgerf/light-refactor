package dk.brics.lightrefactor.types

import dk.brics.lightrefactor.Asts
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.LinkedList
import java.util.List
import java.util.Map
import java.util.Set
import java.util.Stack
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
import org.mozilla.javascript.ast.ScriptNode
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
  
  private val Asts asts
  private extension val FunctionLca lca
  private val TypeUnifier unifier 
  
  new (Asts asts) {
    this.asts = asts
    this.lca = new FunctionLca(asts)
    this.unifier = new TypeUnifier(lca)
  }
  
  private var Set<ScriptNode> ignored = Collections::emptySet
  
  def ignoreNodes(Set<ScriptNode> ignored) {
    this.ignored = ignored
  }
  
  
  private val potentialMethods = new ArrayList<TypeNode> // occurs in pairs of two: host followed by receiver
  
  private extension val TypingImpl typing = new TypingImpl
  
  private static val VOID = true
  private static val NOT_VOID = false
  
  private static val PRIMITIVE = true
  private static val NOT_PRIMITIVE = false
  
  private val objectProto = new TypeNode(null)
  private val functionProto = new TypeNode(null)

  private val functionStack = new Stack<FunctionNode>
  private def currentContext() {
    functionStack.peek()
  }

  private def unify(TypeNode x, TypeNode ... ys) {
    for (y : ys) {
      unifier.unify(x, y)
    }
  }
  
  private def unfold(AstNode exp) {
    var e = exp
    while (e instanceof ParenthesizedExpression) {
      e = (e as ParenthesizedExpression).expression
    }
    return e
  }
  
  private def markNamespace(AstNode node) {
    node.typ.namespace = true
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
  
  private def thisType(FunctionNode f) {
    return f.getVar("@this")
  }
  private def returnType(FunctionNode f) {
  	return f.getVar("@return")
  }
  
  private def addFunction(TypeNode t, FunctionNode f) {
    val node = new LinkNode<FunctionNode>(f)
    if (t.functions == null) {
      t.functions = node
    } else {
      t.functions.splice(node)
    }
    for (i : 0 ..< f.params.size) {
      unify(t.getPrty("@param" + i), f.params.get(i).typ)
    }
    unify(t.getPrty("@return"), f.returnType)
  }
  
  
  def boolean visitExp(AstNode exp, boolean voidctx) {
    switch exp {
      ArrayLiteral: {
          for (AstNode elem : exp.elements) {
            visitExp(elem, NOT_VOID)
            unify(exp.typ.getPrty("@array"), elem.typ)
          }
          return NOT_PRIMITIVE
       }
      ConditionalExpression: {
          visitExp(exp.testExpression, VOID)
          val p1 = visitExp(exp.trueExpression, voidctx)
          val p2 = visitExp(exp.falseExpression, voidctx)
          if (!voidctx) {
            unify(exp.typ, exp.trueExpression.typ, exp.falseExpression.typ)
          }
          return p1 && p2
        }
      ElementGet: {
          visitExp(exp.target, NOT_VOID)
          visitExp(exp.element, NOT_VOID)
          if (exp.element instanceof StringLiteral) {
            unify(exp.target.typ.getPrty((exp.element as StringLiteral).value), exp.typ)
          }
          unify(exp.element.typ.getPrty("@prty-of"), exp.target.typ) // x[p] = y[p] ==> x=y
          return NOT_PRIMITIVE
        }
      EmptyExpression:
        return PRIMITIVE
      FunctionCall: {
        visitExp(exp.target, NOT_VOID)
        val target = unfold(exp.target)
        for (i : 0 ..< exp.arguments.size) {
          val arg = exp.arguments.get(i)
          visitExp(arg, NOT_VOID)
          arg.typ.makeSubtypeOf(target.typ.getPrty("@param" + i))
        }
        exp.typ.makeSubtypeOf(target.typ.getPrty("@return"))
        switch target {
          FunctionNode: {
            val numArgs = Math::min(exp.arguments.size, target.paramCount)
            for (i : 0..<numArgs) {
              unify(exp.arguments.get(i).typ, target.params.get(i).typ)
            }
            unify(target.returnType, exp.typ)
            if (exp instanceof NewExpression) {
              unify(target.getVar("@this"), exp.typ)
            } else {
              unify(target.getVar("@this"), global)
            }
          }
        }
        if (exp instanceof NewExpression) {
          markConstructor(target)
          exp.typ.makeSubtypeOf(target.typ.getPrty("prototype"))
        }
        return NOT_PRIMITIVE
      }
      Assignment: {
        visitExp(exp.left, NOT_VOID)
        val prim = visitExp(exp.right, NOT_VOID)
        if (!prim && exp.type == Token::ASSIGN) {
          unify(exp.typ, exp.left.typ, exp.right.typ)
          
          // check for method definition: e.f = function() {..}
          if (exp.left instanceof PropertyGet && exp.right instanceof FunctionNode) {
            val prtyget = exp.left as PropertyGet
            val fun = exp.right as FunctionNode
            addPotentialMethod(prtyget.left.typ, fun.getVar("@this"))
          }
          
          return NOT_PRIMITIVE
        } else {
          return PRIMITIVE
        }
      }
      PropertyGet: {
        visitExp(exp.left, NOT_VOID) // note: do not visit right, because it is not an expression
        unify(exp.left.typ.getPrty(exp.right.string), exp.typ)
        // if `e.prototype`, then mark `e` as a constructor
        if (exp.right.string == "prototype") {
          markConstructor(exp.left)
        }
        return NOT_PRIMITIVE
      }
      InfixExpression case exp.type == Token::AND: { // && operator
        visitExp(exp.left, VOID)
        val prim = visitExp(exp.right, voidctx)
        unify(exp.typ, exp.right.typ)
        return prim
      }
      InfixExpression case exp.type == Token::OR: { // || operator
        val p1 = visitExp(exp.left, voidctx)
        val p2 = visitExp(exp.right, voidctx)
        if (!voidctx) {
          unify(exp.typ, exp.left.typ, exp.right.typ)
        }
        return p1 && p2
      }
      InfixExpression case exp.type == Token::COMMA: {
        visitExp(exp.left, VOID)
        val p = visitExp(exp.right, voidctx)
        unify(exp.typ, exp.right.typ)
        return p
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
        if (!exp.name.equals("")) {
          unify(exp.getVar(exp.name), exp.typ)
        }
        exp.typ.addFunction(exp)
        visitFunction(exp)
        return NOT_PRIMITIVE
      }
      KeywordLiteral case exp.type == Token::THIS: {
        val fun = exp.enclosingFunction
        if (fun != null) {
          unify(fun.getVar("@this"), exp.typ)
        } else {
          unify(global, exp.typ) // this at top-level is the global object
        }
        return NOT_PRIMITIVE
      }
      KeywordLiteral: // true, false, null
        return PRIMITIVE
      Name: {
        if (exp.string == "undefined") {
          return PRIMITIVE
        } else {
          val scope = if (exp.string == "arguments") exp.enclosingFunction else exp.definingScope
          unify(scope.getVar(exp.string), exp.typ)
          return NOT_PRIMITIVE
        }
      }
      NumberLiteral:
        return PRIMITIVE
      ObjectLiteral: {
        val obj = exp.typ
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
              unify(fun.getVar("@this"), obj)
              unify(obj.getPrty(name), fun.returnType)
            } else if (prop.setter) {
              val fun = prop.right as FunctionNode
              unify(fun.getVar("@this"), obj)
              if (fun.paramCount >= 1) {
                unify(obj.getPrty(name), fun.getVar(fun.params.get(0).string))
              }
            } else { // initializer
              unify(obj.getPrty(name), prop.right.typ)
              
              // check for method definition: { f: function() {..} }
              if (prop.right instanceof FunctionNode) {
                val fun = prop.right as FunctionNode
                addPotentialMethod(obj, fun.getVar("@this"))
              }
            }
          }
        }
        return NOT_PRIMITIVE
      }
      ParenthesizedExpression: {
        val p = visitExp(exp.expression, voidctx)
        unify(exp.typ, exp.expression.typ)
        return p
      }
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
        val scope = stmt.enclosingScope
        unify(scope.getVar(stmt.name), stmt.typ)
        stmt.typ.addFunction(stmt)
        visitFunction(stmt)
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
            unify(fun.returnType, stmt.returnValue.typ)
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
          visitExp(vinit.target, VOID)
          if (vinit.initializer != null) {
            visitExp(vinit.initializer, NOT_VOID)
            unify(vinit.target.typ, vinit.initializer.typ)
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
  
  private def visitFunction(FunctionNode fun) {
    fun.typ.makeSubtypeOf(functionProto)
    thisType(fun).makeSubtypeOf(fun.typ.getPrty("prototype"))
    for (i : 0..<fun.paramCount) {
      val parm = fun.params.get(i)
      unify(fun.getVar(parm.string), parm.typ)
    }
    if (!ignored.contains(fun)) {
      functionStack.push(fun)
      visitStmt(fun.body)
      functionStack.pop()
    }
  }
  
  private def void visitAST(AstRoot root) {
    if (ignored.contains(root))
      return;
    functionStack.push(null)
    for (stmt : root.statements) {
      visitStmt(stmt)
    }
    functionStack.pop()
  }
  
  val initialSubtypes = new LinkedList<TypeNode>
  private def makeSubtypeOf(TypeNode x, TypeNode y) { // adds x <: y
    initialSubtypes.add(x)
    initialSubtypes.add(y)
  }
  
  /** Builds a set of all representative type nodes */
  private def makeTypeSet() {
  	val result = new HashSet<TypeNode>
  	val worklist = new LinkedList<TypeNode>
  	worklist.add(global.rep)
  	result.add(global.rep)
  	for (t : typing.typeMap.values) {
  		if (result.add(t.rep)) {
  			worklist.add(t.rep)
  		}
  	}
  	while (!worklist.isEmpty) {
  		val t = worklist.pop()
  		for (en : t.prty.entrySet) {
  			val w = en.value.rep
  			if (result.add(w)) {
  				worklist.add(w)
  			}
  		}
  		for (w : t.supers) {
  			if (result.add(w.rep)) {
  				worklist.add(w.rep)
  			}
  		}
  	}
  	return result
  }
  
  var clonePhase = 1
  
  private def TypeNode makeClone(TypeNode _self, FunctionNode boundCtx, FunctionNode dstCtx) {
    val self = _self.rep();
    if (self.cloneNr == clonePhase) {
      return self.clone;
    } else {
      self.cloneNr = clonePhase
      if (self.context === boundCtx) {
        self.clone = new TypeNode(dstCtx);
        self.clone.isClone = true;
        // self.clone.supers.addAll(supers); // supers are not cloned [TODO: try with supers]
        for (en : self.prty.entrySet()) {
          val v = en.getValue().rep();
          if (!v.isClone) {
            val clone = v.makeClone(boundCtx, dstCtx)
            if (clone != null) {
              self.clone.prty.put(en.getKey(), clone);
            }
          }
        }
      } else if (self.context.isAncestorOf(dstCtx)) {
        self.clone = self
      } else {
        self.clone = null
      }
      return self.clone;
    }
  }
  
  private def void unifyUnlessNull(TypeNode x, TypeNode y) {
    if (y != null) {
      unifier.unifyLater(x,y)
    }
  }
  
  private def inlineCall(FunctionCall call, FunctionNode target) {
    val callScope = call.getEnclosingFunction();
    clonePhase = clonePhase + 1
    val numArgs = Math::min(call.arguments.size, target.params.size)
    for (j : 0 ..< numArgs) {
      val arg = call.arguments.get(j).typ
      val parm = target.params.get(j).typ
      unifyUnlessNull(arg, parm.makeClone(target, callScope))
    }
    unifyUnlessNull(call.typ, target.returnType.makeClone(target, callScope))
    if (call instanceof NewExpression) {
      unifyUnlessNull(call.typ, target.thisType.makeClone(target, callScope))
    } else {
      val targetExp = call.target
      switch targetExp {
        PropertyGet:
          unifyUnlessNull(targetExp.left.typ, target.thisType.makeClone(target, callScope))
        ElementGet:
          unifyUnlessNull(targetExp.target.typ, target.thisType.makeClone(target, callScope))
      }
    }
  }
  
  private def void addTransitiveSupers(TypeNode _t, Set<TypeNode> result) {
    val t = _t.rep
    if (result.add(t)) {
      for (_sup : t.supers) {
        val sup = _sup.rep
        addTransitiveSupers(sup, result)
      }
    }
  }
  private def getTransitiveSupers(TypeNode t) {
    val result = new HashSet<TypeNode>
    addTransitiveSupers(t, result)
    return result
  }
  
  private def inlineCalls(List<FunctionCall> calls) {
    for (call : calls) {
      for (targetTyp : call.target.typ.transitiveSupers) { // handle inherited call targets
        for (target : targetTyp.getFunctions) {
          inlineCall(call, target)
        }  
      }
  	}
  }
  
  
  private def void purgeCloneDfs(TypeNode node, Set<TypeNode> visited) {
    if (node.parent != node)
      throw new RuntimeException("Must call with root")
    if (!visited.add(node)) {
      return;
    }
    {
      val itr = node.prty.entrySet.iterator
      while (itr.hasNext) {
        val en = itr.next()
        val target = en.value.rep
        if (target.isClone) {
          itr.remove()
        }
      }
    }
    {
      val itr = node.supers.iterator
      val newSupers = new ArrayList<TypeNode>
      while (itr.hasNext) {
        val sup = itr.next().rep
        purgeCloneDfs(sup, visited)
        if (sup.isClone) {
          itr.remove()
          newSupers.addAll(sup.supers)
        }
      }
      node.supers.addAll(newSupers)
    }
  }
  
  private def void purgeClones(Iterable<TypeNode> nodes) {
    val visited = new HashSet<TypeNode>
    for (t : nodes) {
      purgeCloneDfs(t.rep, visited)
    }
  }
  
  private def finish() {
    // collect all function calls
    val calls = new ArrayList<FunctionCall>
    asts.visitAll [ node |
      switch node {
        FunctionCall:
          calls.add(node)
      }
    ]
    
    // NATIVE MODEL
    global.getPrty("Object").getPrty("prototype").unify(objectProto)
    global.getPrty("Function").getPrty("prototype").unify(functionProto)
    functionProto.makeSubtypeOf(objectProto)
    global.getPrty("window").unify(global)
    unifier.complete()
    val extend = global.getPrty("jQuery").getPrty("extend")
    val classCreate = global.getPrty("Class").getPrty("create")
    val definePrty = global.getPrty("Object").getPrty("defineProperty")
    for (call : calls) {
      if (call.target.typ === extend) {
        val t = call.typ
        for (arg : call.arguments) {
          unifier.unifyLater(t, arg.typ)
        }
      }
      else if (call.target.typ === classCreate) {
        val t = call.typ
        if (call.arguments.size === 1) {
          unifier.unifyLater(t.getPrty("prototype"), call.arguments.get(0).typ)
        }
        else if (call.arguments.size == 2) {
          val sup = call.arguments.get(0).typ
          val classBody = call.arguments.get(1).typ
          unifier.unifyLater(t.getPrty("prototype"), classBody)
          classBody.makeSubtypeOf(sup.getPrty("prototype"))
        }
      }
      else if (call.target.typ === definePrty) {
        if (call.arguments.size === 3) {
          val obj = call.arguments.get(0).typ
          val name = call.arguments.get(1)
          val descriptor = call.arguments.get(2).typ
          if (name instanceof StringLiteral) {
            val nameStr = (name as StringLiteral).value
            unify(obj.getPrty(nameStr), descriptor.getPrty("value"))
          }
        }
      }
    }
    
    unifier.complete()
    var i=0
    while (i < potentialMethods.size) {
      val x = potentialMethods.get(i).rep()
      val y = potentialMethods.get(i+1).rep()
      if (!x.namespace) { // note: the line below is more robust, but this variant was used in the original experiments
//      if (!x.namespace && !y.namespace) {
          y.makeSubtypeOf(x)
//        unifier.unifyLater(x,y)
      }
      i = i + 2
    }
    potentialMethods.clear()
    unifier.complete()
    
    while (!initialSubtypes.isEmpty) {
      val y = initialSubtypes.removeLast().rep()
      val x = initialSubtypes.removeLast().rep() // x <: y
      if (x != y)
        x.supers.add(y)
    }
    
    val allTypes = makeTypeSet()
    
    val contextProp = new ContextPropagation(lca)
    
    var changed = true
    while (changed) {
      changed = false
      
      contextProp.propagate(allTypes)
      
      inlineCalls(calls)
      unifier.complete();
      
      purgeClones(allTypes)
      
		  val subt = new SubtypeInference(unifier);
    	changed = subt.inferSubTypes(allTypes) || changed
    	
    	// Opportunistic call inlining
//    	changed = inlineCalls(calls) || changed

//      changed = false
    }
    
//    
    for (TypeNode _typ : allTypes) {
      val typ = _typ.rep
      for (TypeNode sup : typ.supers) {
        if (sup.rep() != typ) {
          typing.subtypes.getSet(sup.rep).add(typ)
        }
      }
    }
    
  }
  
  def Typing inferTypes() {
    for (ast : asts) {
      visitAST(ast)
    }
    finish()
    typing
  }
  
  
}

class TypingImpl implements Typing {
  val TypeNode global = new TypeNode(null)
  public val typeMap = new HashMap<AstNode, TypeNode>
  val scopeMap = new HashMap<Scope, TypeNode>
  public val subtypes = new HashMap<TypeNode, HashSet<TypeNode>>
//  public val supertypes = new HashMap<TypeNode, ArrayList<TypeNode>>
  
  override def rootTypes() {
    typeMap.values().map[it.rep]
  }
  
  override def superTypes(TypeNode t) {
    return t.rep.supers.map[it.rep]
//    return supertypes.getList(t.rep)
  }
  override def subTypes(TypeNode t) {
    return subtypes.getSet(t.rep)
  }
  
  override def global() {
    global
  }
  
  override def typ(AstNode n) {
    var t = typeMap.get(n)
    if (t == null) {
      t = new TypeNode(n.enclosingFunction) // TODO optimize: use function stack during AST traversal 
      typeMap.put(n, t)
    } else {
      t = t.rep()
    }
    t
  }
  
  public def getScopeObj(Scope scope) {
    if (scope.globalScope) {
      global.rep()
    } else {
      var obj = scopeMap.get(scope)
      if (obj == null) {
        obj = new TypeNode(scope.enclosingFunction)
        scopeMap.put(scope, obj)
      } else {
        obj = obj.rep()
      }
      obj
    }
  }
  
  override def lookupVar(Scope scope, String varName) {
    scope.scopeObj.lookupPrty(varName)
  }
  
  override def getVar(Scope scope, String varName) {
    scope.scopeObj.getPrty(varName)
  }
  override def variables(Scope scope) {
    scope.scopeObj.rep.prty.keySet
  }
  
  override def lookupPrty(TypeNode type, String varName) {
    val t = type.rep.prty.get(varName)
    if (t == null) {
      null
    } else {
      t.rep
    }
  }
  
  override def getPrty(TypeNode _type, String name) {
    val type = _type.rep
    var t = type.prty.get(name)
    if (t == null) {
      t = new TypeNode(type.context)
      type.prty.put(name, t)
    }
    t.rep()
  }
  override def properties(TypeNode type) {
    type.rep.prty.keySet
  }
  
}