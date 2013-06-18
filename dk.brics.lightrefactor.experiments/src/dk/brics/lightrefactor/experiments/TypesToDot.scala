package dk.brics.lightrefactor.experiments
import dk.brics.lightrefactor.Loader
import java.io.File
import dk.brics.lightrefactor.types.TypeInference
import scala.collection.mutable
import dk.brics.lightrefactor.types.TypeNode
import dk.brics.scalautil.graph.Graph
import scala.collection.JavaConversions._
import dk.brics.scalautil.IO

object TypesToDot {
  def main(args:Array[String]) {
    val loader = new Loader
    for (arg <- args) {
      loader.addFile(new File(arg))
    }
    val asts = loader.getAsts
    
    val types = new TypeInference(asts).inferTypes()
    
    val allTypes = Graph.reachable[TypeNode](types.rootTypes.toSet + types.global, v => types.properties(v).map(p => types.getPrty(v,p)))
    
    val sb = new StringBuilder
    def println(fmt:String, args:Any*) {
      sb.append(fmt.format(args :_ *)).append("\n")
    }
    println("digraph {")
    val type2id = new mutable.HashMap[TypeNode, Int]
    def node(t:TypeNode) = type2id.getOrElseUpdate(t, type2id.size)
    for (t <- allTypes) {
      println("  %d [label=\"%s\", shape=box]", node(t), "") // TODO clever labels
      for (prty <- types.properties(t)) {
        val w = types.getPrty(t,prty)
        println("  %d -> %d [label=\"%s\"]", node(t), node(w), DotUtil.escapeLabel(prty))
      }
      for (w <- types.superTypes(t)) {
        println("  %d -> %d [arrowhead=empty]", node(t), node(w))
      }
    }
    println("}")
    
    val outfile = new File(EvalUtil.outputDir, "types.dot")
    IO.writeStringToFile(outfile, sb.toString)
    Console.println(outfile.getPath)
  }
}