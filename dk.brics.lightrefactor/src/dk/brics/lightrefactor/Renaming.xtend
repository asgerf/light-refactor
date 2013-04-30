package dk.brics.lightrefactor

import dk.brics.lightrefactor.types.TypeInference
import dk.brics.lightrefactor.types.TypeNode
import java.util.ArrayList
import java.util.HashMap
import java.util.List
import org.mozilla.javascript.Function
import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.Label
import org.mozilla.javascript.ast.LabeledStatement
import org.mozilla.javascript.ast.Name

import static extension dk.brics.lightrefactor.NameRef.*
import static extension dk.brics.lightrefactor.util.MapExtensions.*

class Renaming {
  val Asts asts
  val AstNode targetNode
  var List<ArrayList<AstNode>> questions;
  extension var TypeInference types
  
  def getQuestions() { questions }
  
  new (Asts asts, AstNode targetNode) {
    this.asts = asts
    this.targetNode = targetNode
    
    if (targetNode.isPrty) {
      this.computePropertyQuestions()
    } else if (targetNode.isLocalVar) {
      this.computeLocalQuestions()
    } else if (targetNode.isGlobalVar) {
      this.computeGlobalQuestions()
    } else if (targetNode.isLabel) {
      this.computeLabelQuestions()
    } else {
      throw new IllegalArgumentException("Cannot rename node: " + targetNode.class.simpleName)
    }
  }
  
  private def void computeTypes() {
    if (types == null) {
      types = new TypeInference(asts)
    }
  }
  
  private def void computePropertyQuestions() {
    computeTypes()
    val originalName = targetNode.name
    
    if (targetNode.base.typ == types.global) {
      computeGlobalQuestions()
      return
    }
    
    // collect all property name tokens
    val names = new ArrayList<AstNode>
    asts.visitAll [ node | switch node { case node.isPrty && node.name == originalName: names.add(node) } ]
    
    // map types to their references
    val base2names = new HashMap<TypeNode, ArrayList<AstNode>>
    for (name : names) {
      base2names.getList(name.base.typ).add(name)
    }
    
    // order the tokens within each group by line number 
//    base2names.values.forEach[it.sortInplaceBy[it.lineno]]
    
    // get the list of groups, ordered by the earliest first occurrence
    this.questions = base2names.values.toList //.sortBy[it.get(0).lineno].toList
  }
  
  private def void computeLocalQuestions() {
    val scope = (targetNode as Name).definingScope
    val names = new ArrayList<AstNode>
    scope.visit [ node | 
      switch node { 
        Name case node.isVar && node.name == targetNode.name && node.definingScope == scope: 
          names.add(node)
      }
      true
    ]
    this.questions = #[names]
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
  
  private def void computeLabelQuestions() {
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
    questions = new ArrayList
    questions.add(tokens)
  }
  
  private def void computeGlobalQuestions() {
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
    
    questions = new ArrayList
    questions.add(names)
    if (objNames.size > 0) {
      questions.add(objNames)
    }
  }
  
  
}
