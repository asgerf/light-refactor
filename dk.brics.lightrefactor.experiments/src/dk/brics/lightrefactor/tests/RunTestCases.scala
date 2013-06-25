package dk.brics.lightrefactor.tests
import java.io.File
import dk.brics.lightrefactor.Loader
import dk.brics.lightrefactor.Renaming
import org.mozilla.javascript.ast.NodeVisitor
import org.mozilla.javascript.ast.AstNode
import scala.collection.mutable
import dk.brics.lightrefactor.NameRef

object RunTestCases {
	def main(args: Array[String]) {
	  val dir = new File("testcases")
	  for (file <- dir.listFiles() if file.isFile && file.getName.endsWith(".js")) {
	    Console.println(file.getName)
	    val loader = new Loader
	    loader.addFile(file)
	    val asts = loader.getAsts
	    
	    val renaming = new Renaming(asts)
	    val names = new mutable.HashSet[String]
	    asts.visit(new NodeVisitor {
	      override def visit(node:AstNode) = {
	        if (NameRef.isPrty(node)) {
	          names += NameRef.name(node)
	        }
	        true
	      }
	    })
	    
	    for (name <- names) {
	      val quests = renaming.renameProperty(name)
	      Console.println(name + " " + quests.size)
	    }
	  }
	}
}