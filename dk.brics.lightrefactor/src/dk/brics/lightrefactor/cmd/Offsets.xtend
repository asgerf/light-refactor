package dk.brics.lightrefactor.cmd

import dk.brics.jshtml.LineOffsets
import org.mozilla.javascript.ast.AstNode

class Offsets {
  val LineOffsets offsets
  new (String str) {
    offsets = new LineOffsets(str)
  }
  
  def linepos(AstNode node) {
    node.absolutePosition - offsets.getStartOfLine(node.lineno)
  }
}