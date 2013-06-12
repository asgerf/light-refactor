package dk.brics.lightrefactor

import dk.brics.lightrefactor.types.Equivalence
import dk.brics.lightrefactor.types.TypeInference
import dk.brics.lightrefactor.types.TypeNode
import dk.brics.lightrefactor.types.Typing
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Set
import org.mozilla.javascript.Function
import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.Label
import org.mozilla.javascript.ast.LabeledStatement
import org.mozilla.javascript.ast.Name
import org.mozilla.javascript.ast.ScriptNode

import static extension dk.brics.lightrefactor.NameRef.*
import static extension dk.brics.lightrefactor.util.MapExtensions.*

class Renaming {
  val Asts asts
  extension var Typing types
  var Set<ScriptNode> ignored = new HashSet<ScriptNode>
  
  new (Asts asts) {
    this.asts = asts
  }
  new (Asts asts, Typing types) {
    this.asts = asts
    this.types = types
  }
  
  def void ignoreNodes(Iterable<? extends ScriptNode> nodes) {
    ignored.addAll(nodes)
  }
  
  def List<ArrayList<AstNode>> renameNode(AstNode targetNode) {
    if (targetNode.isPrty) {
      this.computePropertyQuestions(targetNode)
    } else if (targetNode.isLocalVar) {
      this.computeLocalQuestions(targetNode)
    } else if (targetNode.isGlobalVar) {
      this.computeGlobalQuestions(targetNode)
    } else if (targetNode.isLabel) {
      this.computeLabelQuestions(targetNode)
    } else {
      throw new IllegalArgumentException("Cannot rename node: " + targetNode.class.simpleName)
    }
  }
  
  def List<ArrayList<AstNode>> renameProperty(String prty) {
    computePropertyQuestionsWithName(prty)
  }
  
  private def void computeTypes() {
    if (types == null) {
      val inf = new TypeInference(asts)
      inf.ignoreNodes(ignored)
      types = inf.inferTypes()
    }
  }
  
  private def List<ArrayList<AstNode>> computePropertyQuestions(AstNode targetNode) {
    computeTypes()
    val originalName = targetNode.name
    
    if (targetNode.base.typ == types.global) {
      return computeGlobalQuestions(targetNode)
    } else {
      return computePropertyQuestionsWithName(originalName)
    }
  }
  
  private def List<ArrayList<AstNode>> computePropertyQuestionsWithName(String originalName) {  
    computeTypes()
    
    // collect all property name tokens
    val names = new ArrayList<AstNode>
    asts.visitAll [ node | switch node { case node.isPrty && node.name == originalName: names.add(node) } ]
    
    // map types to their references
    val base2names = new HashMap<TypeNode, ArrayList<AstNode>>
    for (name : names) {
      base2names.getList(name.base.typ).add(name)
    }
    
    // unify types that are connected by subtyping and both refer to the original property name
    val equiv = new Equivalence<TypeNode>
    for (typ : base2names.keySet) {
      for (sub : typ.subTypes) {
        if (base2names.containsKey(sub)) {
          equiv.unify(typ, sub);
        }
      }
    }
    
    val rep2names = new HashMap<TypeNode, ArrayList<AstNode>>
    for (typ : base2names.keySet) {
      val rep = equiv.getRep(typ)
      rep2names.getList(rep).addAll(base2names.get(typ))
    }
    
    return rep2names.values.toList
  }
  
  private def List<ArrayList<AstNode>> computeLocalQuestions(AstNode targetNode) {
    val scope = (targetNode as Name).definingScope
    val names = new ArrayList<AstNode>
    scope.visit [ node | 
      switch node { 
        Name case node.isVar && node.name == targetNode.name && node.definingScope == scope: 
          names.add(node)
      }
      true
    ]
    val list = new ArrayList<ArrayList<AstNode>>
    list.add(names)
    return list
  }
  
  private def LabeledStatement getTargetStmt(AstNode labelName) {
    val name = labelName.name
    var AstNode node = labelName
    while (node != null) {
      switch node {
        LabeledStatement case node.getLabelByName(name) != null:
          return node 
        Function:
          return null // cannot target label outside function boundaries
      }
      node = node.parent
    }
    null
  }
  
  private def List<ArrayList<AstNode>> computeLabelQuestions(AstNode targetNode) {
    val name = targetNode.name
    val labeledStmt = getTargetStmt(targetNode)
    val tokens = new ArrayList<AstNode>
    labeledStmt.visit [ node |
      switch node {
        Name case node.isLabel && node.string == name:
          tokens.add(node)
        Label case node.name == name:
          tokens.add(node)
        LabeledStatement case node != labeledStmt && labeledStmt.getLabelByName(name) != null:
          return false // stop further recursion when label gets shadowed
        Function:
          return false // stop further recursion when leaving function boundaries
      }
      true
    ]
    val questions = new ArrayList
    questions.add(tokens)
    return questions
  }
  
  private def List<ArrayList<AstNode>> computeGlobalQuestions(AstNode targetNode) {
    computeTypes()
    val originalName = targetNode.name
    
    val global = types.global
    
    // collect all property name tokens
    val names = new ArrayList<AstNode>
    val objNames = new ArrayList<AstNode>
    asts.visitAll [ node | 
      switch node { 
        case node.isPrty && node.base.typ == global && node.name == originalName: 
          objNames.add(node)
        case node.isGlobalVar && node.name == originalName:
          names.add(node)
      }
    ]
    
    val questions = new ArrayList
    questions.add(names)
    if (objNames.size > 0) {
      questions.add(objNames)
    }
    return questions
  }
  
  
}
