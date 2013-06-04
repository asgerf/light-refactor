package dk.brics.lightrefactor.experiments
import dk.brics.lightrefactor.Loader
import java.io.File
import scala.collection.mutable
import dk.brics.lightrefactor.VisitAll
import org.mozilla.javascript.ast.AstNode
import dk.brics.lightrefactor.NameRef
import dk.brics.lightrefactor.Renaming
import scala.collection.JavaConversions._

object MeasureGroupSizes {
  
  import EvalUtil._
  
  def main(args:Array[String]) {
    val ranges = new mutable.ListBuffer[Range]
    for (arg <- args) {
      if (arg.startsWith("-")) {
        Console.err.println("Unrecognized option: " + arg)
        System.exit(1)
      } else {
        try {
          ranges ++= arg.split(" ").map(parseRange)
        } catch {
          case e => Console.err.println(e.getMessage); System.exit(1)
        }
      }
    }
    if (ranges.size == 0) {
      ranges ++= "1 2 3..5 6..8 9+".split(" ").map(parseRange)
    }
    Console.printf("%-20s%s\n", "#benchmark", ranges.map(q => "%6s".format(q)).mkString(""))
    for (dir <- benchmarkDirs(includeLibs=false)) {
      val loader = new Loader
      if (new File(dir,"index.html").exists) {
        loader.addFile(new File(dir, "index.html"))
      } else {
        for (file <- allJavaScriptFiles(dir)) {
          loader.addFile(file)
        }
      }
      val asts = loader.getAsts
      
      // collect all property names
      val propertyNames = new mutable.HashSet[String]
      asts.visit(new VisitAll {
        override def handle(node:AstNode) {
          if (NameRef.isPrty(node)) {
            propertyNames += NameRef.name(node)
          }
        }
      })
      
      val size2freq = new mutable.HashMap[Int,Int]
      val rename = new Renaming(asts)
      for (name <- propertyNames if name != "prototype") {
        val questions = rename.renameProperty(name)
        for (group <- questions) {
          size2freq += group.size -> (1 + size2freq.getOrElse(group.size,0))
        }
      }
      
      Console.printf("%-20s", dir.getName)
      for (range <- ranges) {
        var count = 0
        for (n <- range.from until range.to) {
          count += n * size2freq.getOrElse(n,0)
        }
        Console.printf("%6d", count)
      }
      Console.println()
    }
  }
  
}