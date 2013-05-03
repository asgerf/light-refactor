package dk.brics.lightrefactor

import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.AstRoot
import org.mozilla.javascript.ast.BreakStatement
import org.mozilla.javascript.ast.ContinueStatement
import org.mozilla.javascript.ast.ForInLoop
import org.mozilla.javascript.ast.Label
import org.mozilla.javascript.ast.Name
import org.mozilla.javascript.ast.NumberLiteral
import org.mozilla.javascript.ast.ObjectProperty
import org.mozilla.javascript.ast.PropertyGet
import org.mozilla.javascript.ast.Scope
import org.mozilla.javascript.ast.StringLiteral
import org.mozilla.javascript.ast.VariableDeclaration

class NameRef {
  /** The contents of `name` if it is a Name, StringLiteral, or Label */
  def static String name(AstNode name) {
    switch name {
      Name: name.string
      StringLiteral: name.value
      Label: name.name
      default: throw new IllegalArgumentException(name.class.simpleName + " is not a name reference")
    }
  }
  
  /** If `name` is part of a property access, returns the base expression, otherwise returns null */
  def static AstNode base(AstNode name) {
    val parent = name.parent
    switch parent {
      PropertyGet case parent.right == name: parent.left
      ObjectProperty case parent.left == name && !(name instanceof NumberLiteral): parent.parent
      default: null
    }
  }
  
  /** True if `name` is part of a property access */
  def static boolean isPrty(AstNode name) { 
    base(name) != null
  }
  
  def static boolean isLabel(AstNode name) {
    val parent = name.parent
    switch parent {
      BreakStatement case parent.breakLabel == name: true
      ContinueStatement case parent.label == name: true
      default: name instanceof Label
    }
  }
  
  def static boolean isVar(AstNode name) {
    name instanceof Name && !isPrty(name) && !isLabel(name)
  }
  
  def static boolean isLocalVar(AstNode name) {
    isVar(name) && !isGlobalScope((name as Name).definingScope)
  }
  
  def static boolean isGlobalVar(AstNode name) {
    isVar(name) && isGlobalScope((name as Name).definingScope)
  }
  
  def static boolean isGlobalScope(Scope scope) {
    scope == null || scope instanceof AstRoot
  }
  
  def static int startPos(AstNode name) {
    switch name {
      Name: name.absolutePosition
      StringLiteral: 1 + name.absolutePosition
      Label: name.absolutePosition
    }
  }
  
  /**
   * The variable Name node or expression that is the left-hand side in the given for-in loop. 
   */
  def static iteratorNode(ForInLoop stmt) {
    if (stmt.getIterator instanceof VariableDeclaration) {
      val decl = stmt.getIterator as VariableDeclaration
      decl.variables.head.target
    } else {
      stmt.getIterator
    }
  }
}
