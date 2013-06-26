package dk.brics.lightrefactor.tests
import java.io.File
import dk.brics.lightrefactor.Loader
import dk.brics.lightrefactor.Renaming
import org.mozilla.javascript.ast.NodeVisitor
import org.mozilla.javascript.ast.AstNode
import scala.collection.mutable
import dk.brics.lightrefactor.NameRef
import dk.brics.lightrefactor.JsSource
import dk.brics.lightrefactor.util.IO
import scala.collection.JavaConversions._
import dk.brics.lightrefactor.experiments.Equivalence

object RunTestCases {
	def main(args: Array[String]) {
	  val dir = new File("testcases")
	  for (file <- dir.listFiles() if file.isFile && file.getName.endsWith(".js")) {
	    val loader = new Loader
	    loader.addFile(file)
	    val asts = loader.getAsts
	    
	    val text = IO.readFile(file)
	    val root = asts.get(new JsSource(file))
	    val testgroups = TestCaseGroups.find(text, root)
	    
	    val prtyNames = (for (xs <- testgroups.values; x <- xs) yield NameRef.name(x)).toSet
	    val namedTokens = (for (xs <- testgroups.values; x <- xs) yield x).toSet
	    
	    val renaming = new Renaming(asts)
	    
	    for (prty <- prtyNames) {
	      val computedGroups = renaming.renameProperty(prty).map(_.filter(q => namedTokens(q)).toList).toList
	      val assertedGroups = testgroups.values.map(_.filter(q => NameRef.name(q) == prty).toList).toList
	      
	      val computedEq = Equivalence.fromGroups(computedGroups)
	      val assertedEq = Equivalence.fromGroups(assertedGroups)
	      
	      for ((x,y) <- Equivalence.nonSubsetItems(computedEq, assertedEq)) {
	        Console.printf("[unsound]   \"%s\" at %s:%d and %s:%d\n",
	            NameRef.name(x),
	            file.getName,
	            x.getLineno+1,
	            file.getName,
	            y.getLineno+1)
	      }
	      for ((x,y) <- Equivalence.nonSubsetItems(assertedEq, computedEq)) {
          Console.printf("[imprecise] \"%s\" at %s:%d and %s:%d\n",
              NameRef.name(x),
              file.getName,
              x.getLineno+1,
              file.getName,
              y.getLineno+1)
	      }
	    }
	  }
	}
}