package dk.brics.lightrefactor.experiments
import dk.brics.lightrefactor.Loader
import java.io.File
import dk.brics.lightrefactor.VisitAll
import scala.collection.mutable
import scala.collection.JavaConversions._
import org.mozilla.javascript.ast.AstNode
import dk.brics.lightrefactor.NameRef
import dk.brics.lightrefactor.Renaming
import dk.brics.scalautil.IO

object DumpGroupSizes {
  
  import EvalUtil._
  
  def main(args:Array[String]) {
    val file = new File(outputDir, "groups.txt")
    IO.writeFile(file) { case writer => 
      for (dir <- benchmarkDirs(includeLibs=false)) {
        Console.println(dir.getName)
        val loader = new Loader
        loader.addFile(new File(dir,"index.html"))
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
        
        val rename = new Renaming(asts)
        for ((prty,size) <- propertyNames if size > 1 && prty != "prototype") {
          val questions = rename.renameProperty(prty)
          for (group <- questions) {
            writer.write("%-20s %d\n".format(dir.getName, group.size))
          }
        }
      }
    }
    Console.println(file)
  }
}