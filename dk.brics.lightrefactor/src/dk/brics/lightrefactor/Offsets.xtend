package dk.brics.lightrefactor

import java.util.ArrayList
import java.util.List
import org.mozilla.javascript.ast.AstNode

/**
 * Helper object for working with line numbers and offsets in a source file.
 * This class operates with 0-based line numbers, and assumes all line break symbols are one character (i.e. no MS-style \r\n line endings).
 */
class Offsets {
  val offsets = new ArrayList<Integer>
  
  new(List<String> sourceLines) {
    var offset = 0
    for (line : sourceLines) {
      offsets.add(offset)
      offset = offset + line.length + 1 // add 1 for line break symbol (which we force to be \n)
    }
  }
  
  def startOfLine(int lineno) {
    offsets.get(lineno)
  }
  
  /** The given node's position on its line, with 0 being the first character on the line */
  def linepos(AstNode node) {
    node.absolutePosition - startOfLine(node.lineno)
  }
}
