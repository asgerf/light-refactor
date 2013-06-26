package dk.brics.lightrefactor.experiments
import java.io.File
import scala.collection.JavaConversions._
import scala.collection.mutable
import dk.brics.lightrefactor.types.TypeInference
import dk.brics.lightrefactor.types.TypeNode
import dk.brics.lightrefactor.Loader
import dk.brics.scalautil.graph.Graph
import dk.brics.scalautil.IO
import dk.brics.lightrefactor.types.Typing

object TypesToDot {
  def computeNames(types:Typing) : Map[TypeNode,String] = {
    val names = new mutable.HashMap[TypeNode,String]
    val worklist = new mutable.Queue[TypeNode]
    def addName(t:TypeNode, name:String) {
      if (names.containsKey(t))
        return
      names += t -> name
      worklist += t
    }
    for (name <- types.properties(types.global)) {
      addName(types.getPrty(types.global, name), name)
    }
    while (!worklist.isEmpty) {
      val q = worklist.dequeue()
      for (prty <- types.properties(q)) {
        addName(types.getPrty(q,prty), names(q) + "." + prty)
      }
    }
    names.toMap
  }
  
  def main(args:Array[String]) {
    val loader = new Loader
    for (arg <- args) {
      loader.addFile(new File(arg))
    }
    val asts = loader.getAsts
    
    val types = new TypeInference(asts).inferTypes()
    
    val allTypes = Graph.reachable[TypeNode](types.rootTypes.toSet + types.global, v => types.properties(v).map(p => types.getPrty(v,p)))
    
    val names = computeNames(types)
    
    val sb = new StringBuilder
    def println(fmt:String, args:Any*) {
      sb.append(fmt.format(args :_ *)).append("\n")
    }
    println("digraph {")
    println("  concentrate = true")
    println("  colorscheme = pastel19")
    val type2id = new mutable.HashMap[TypeNode, Int]
    def node(t:TypeNode) = type2id.getOrElseUpdate(t, type2id.size)
    def color(t:TypeNode) = "/pastel19/" + (1 + t.hashCode % 9).toString
    for (t <- allTypes) {
      val label = names.getOrElse(t, "")
      println("  %d [shape=record, label=\"{%s|{%s}}\", style=filled, fillcolor=\"%s\"]", 
          node(t), 
          label,
          (for (prty <- types.properties(t)) yield "<" + DotUtil.escapeKey(prty) + "> " + DotUtil.escapeLabel(prty)).mkString("|"),
          color(t)
          )
      for (prty <- types.properties(t)) {
        val w = types.getPrty(t,prty)
        println("  %d:%s -> %d [color=\"%s\"]", node(t), DotUtil.escapeKey(prty), node(w), color(w))
      }
      for (w <- types.superTypes(t)) {
        println("  %d -> %d [arrowhead=empty, color=\"%s\"]", node(t), node(w), color(w))
      }
    }
    println("}")
    
    val outfile = new File(EvalUtil.outputDir, "types.dot")
    IO.writeStringToFile(outfile, sb.toString)
    if (!DotUtil.compileAndOpen(outfile)) {
      Console.println(outfile.getPath)
    }
  }
}