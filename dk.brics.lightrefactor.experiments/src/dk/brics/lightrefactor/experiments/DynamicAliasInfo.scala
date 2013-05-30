package dk.brics.lightrefactor.experiments
import dk.brics.lightrefactor.Asts
import java.io.Reader
import org.mozilla.javascript.ast.AstNode
import scala.io.Source
import dk.brics.scalautil.immut._
import scala.collection.JavaConversions._
import java.io.File
import dk.brics.lightrefactor.JsSource
import dk.brics.lightrefactor.HtmlSource
import scala.collection.mutable
import org.mozilla.javascript.ast.NodeVisitor
import org.mozilla.javascript.ast.PropertyGet
import org.mozilla.javascript.ast.ElementGet
import org.mozilla.javascript.ast.ObjectLiteral

trait DynamicAliasInfo {
  def nodes : Set[AstNode]
  def objects : Set[Int]
  def objectsAt(node:AstNode) : Set[Int]
  def flowsTo(obj:Int) : Set[AstNode]
}

object DynamicAliasInfo {
  def getRelative(base:File, child:File) : String = {
    val baseStr = base.getAbsolutePath
    val childStr = child.getAbsolutePath
    if (!childStr.startsWith(baseStr))
      throw new RuntimeException("Not contained in base directory")
    else
      childStr.substring(baseStr.length+1)
  }
  
  def load(info:Source, asts:Asts, root:File) = {
    val id2node = new mutable.HashMap[String,AstNode]
    for (ast <- asts) {
      val src = asts.source(ast)
      val srcid = src match {
        case src:JsSource => getRelative(root, src.getFile)
        case src:HtmlSource => ""+src.getIndex
      }
      ast.visit(new NodeVisitor {
        var id=0
        def next() = {
          val name = srcid + "@" + id
          id += 1
          name
        }
        override def visit(node:AstNode) = {
          node match {
            case node:PropertyGet =>
              id2node += next() -> node.getLeft
            case node:ElementGet =>
              id2node += next() -> node.getTarget
            case node:ObjectLiteral =>
              id2node += next() -> node
            case _ =>
          }
          true
        }
      })
    }
    var dynbase = MultiMap.empty[AstNode,Int]
    for (line <- info.getLines()) {
      val List(obj,expr) = line.split(", *").toList
//      val node = id2node(expr)
      for (node <- id2node.get(expr)) {
        dynbase += ((node, obj.toInt))
      }
    }
    val base2exp = dynbase.inverse
    new DynamicAliasInfo {
      def nodes = dynbase.keySet
      def objects = base2exp.keySet
      def objectsAt(node:AstNode) = dynbase(node)
      def flowsTo(obj:Int) = base2exp(obj)
    }
  }
}