package dk.brics.lightrefactor.experiments

import scala.collection.mutable
import scala.collection.JavaConversions._
import java.io.File
import dk.brics.lightrefactor.Renaming
import dk.brics.lightrefactor.Loader
import dk.brics.lightrefactor.VisitAll
import org.mozilla.javascript.ast.AstNode
import dk.brics.lightrefactor.NameRef

object MeasureNQuestions {
  
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
      ranges ++= "1 1..2 1..3 1..4 1..5 1..6 1..7 1..8 1..9 1..10 1+".split(" ").map(parseRange)
    }
    class BenchData {
      val num2freq = new mutable.HashMap[Int,Int]
      val naive2freq = new mutable.HashMap[Int,Int]
    }
    val datas = new mutable.HashMap[File, BenchData]
    Console.printf("%-20s %-10s%s\n", "#benchmark", "tool", ranges.map(q => "%6s".format(q)).mkString(""))
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
      val propertyNames = new mutable.HashMap[String,Int]
      asts.visit(new VisitAll {
        override def handle(node:AstNode) {
          if (NameRef.isPrty(node)) {
            val name = NameRef.name(node)
            propertyNames += name -> (1 + propertyNames.getOrElse(name,0))
          }
        }
      })
      val data = new BenchData
      datas += dir -> data
      import data._
      val rename = new Renaming(asts)
      for ((name,z) <- propertyNames if name != "prototype" && z > 1) {
        val questions = rename.renameProperty(name)
        num2freq += questions.size -> (1 + num2freq.getOrElse(questions.size,0))
        val count = questions.map(_.size).sum
        naive2freq += count -> (1 + naive2freq.getOrElse(count,0))
      }
    }
    for ((dir,data) <- datas.toList.sortBy(_._1)) {
      import data._
      Console.printf("%-20s %-10s", dir.getName, "naive")
      for (range <- ranges) {
        var count = 0
        for (n <- range.from until range.to) {
          count += naive2freq.getOrElse(n,0)
        }
        Console.printf("%6d", count)
      }
      Console.println()
    }
    for ((dir,data) <- datas.toList.sortBy(_._1)) {
      import data._
      Console.printf("%-20s %-10s", dir.getName, "rename")
      for (range <- ranges) {
        var count = 0
        for (n <- range.from until range.to) {
          count += num2freq.getOrElse(n,0) -  naive2freq.getOrElse(n,0)
        }
        Console.printf("%6d", count)
      }
      Console.println()
    }
  }
  
}