package dk.brics.lightrefactor

import java.io.File
import java.util.HashMap
import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.AstRoot
import org.mozilla.javascript.ast.NodeVisitor

/**
 * Value denoting where an AST came from.
 */
interface ISource {
  /** 
   * The file from which the source originated, or <tt>null</tt> if not applicable.
   * <p/>
   * This method is provided for convenience, in cases where only JsSource and HtmlSource are used.
   * When using other types of ISource instances, this method should not be used, and may be
   * implemented by returning <tt>null</tt>.
   */
  def File getFile();
  
  def boolean equals(Object o);
  def int hashCode();
}

/**
 * Denotes a JavaScript source file.
 */
@Data class JsSource implements ISource {
  val File file
  override def toString() { file.getName }
}

/**
 * Denotes a JavaScript fragment inside an HTML file.
 */
@Data class HtmlSource implements ISource {
  val File file
  val int offset
  val int lineno
  val int index
  override def toString() { file.getName + "@" + lineno }
}

/**
 * Generic source object that does not equal any other object.
 * Can be used when no useful source information is available.
 */
class GenericSource implements ISource {
  override def File getFile() { null }
}

/**
 * Describes a position inside a JavaScript file or fragment.
 */
@Data class Location {
  val ISource source
  val int position
}

abstract class VisitAll implements NodeVisitor {
  override final def visit(AstNode node) {
    handle(node)
    true
  }
  abstract def void handle(AstNode node)
}

/**
 * A collection of ASTs mapped to their corresponding sources.
 */
class Asts implements Iterable<AstRoot> {
  private val src2ast = new HashMap<ISource,AstRoot>
  private val ast2src = new HashMap<AstRoot,ISource>
  
  override def iterator() {
    ast2src.keySet.iterator()
  }
  
  /** Applies the given visitor to each AST */
  def visit(NodeVisitor visitor) {
    for (ast : this) {
      ast.visit(visitor)
    }
  }
  /** Applies the given visitor to each AST. Convenience method for use with Xtend's closure syntax. */
  def visitAll(VisitAll visitor) {
    for (ast : this) {
      ast.visit(visitor)
    }
  }
  
  /** The absolute line number of the given node, taking HTML line offsets into account */
  def absoluteLineNo(AstNode node) {
    val src = source(node)
    switch src {
      HtmlSource:
        return src.lineno + node.lineno
      default:
        return node.lineno
    }
  }
  /** The source from which the given node originated (same for all nodes in a given AST) */
  def source(AstNode node) {
    ast2src.get(node.astRoot)
  }
  
  /** Location of the given node (i.e. its source and its absolute position) */
  def location(AstNode node) {
    new Location(node.source, node.absolutePosition)
  }
  
  /** AST of the given source. */
  def get(ISource src) {
    src2ast.get(src)
  }
  
  /** Adds a source-AST mapping. */
  def add(ISource src, AstRoot ast) {
    src2ast.put(src, ast)
    ast2src.put(ast, src)
  }
}