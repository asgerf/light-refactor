package dk.brics.lightrefactor.experiments
import java.io.File
import scala.io.Source
import scala.collection.JavaConversions._
import scala.collection.mutable
import dk.brics.lightrefactor.NameRef
import dk.brics.lightrefactor.JsSource
import dk.brics.lightrefactor.Renaming
import org.mozilla.javascript.ast.AstNode
import java.util.regex.Pattern.Start
import dk.brics.lightrefactor.Loader
import org.mozilla.javascript.Node
import dk.brics.lightrefactor.VisitAll
import dk.brics.scalautil.mut

object CompareWithDynamic {
  def getRelative(base:File, child:File) : String = {
    val baseStr = base.getAbsolutePath
    val childStr = child.getAbsolutePath
    if (!childStr.startsWith(baseStr))
      throw new RuntimeException("Not contained in base directory")
    else
      childStr.substring(baseStr.length+1)
  }
  def printUsage() {
    Console.println("CompareWithDynamic [-showdead] DIR")
    Console.println("  DIR must contain an index.html and dyn-alias.txt file")
    Console.println("  -showdead includes tokens not covered by dynamic execution")
  }
  def main(args:Array[String]) {
    // Parse arguments
    var file : File = null
    var showdead = false
    for (arg <- args) {
      if (arg == "-showdead") {
        showdead = true
      }
      else if (arg.startsWith("-")) {
        Console.err.println("Unrecognized option: " + arg)
        System.exit(1)
      }
      else {
        file = new File(arg)
      }
    }
    if (file == null) {
      printUsage();
      return;
    }
    var inputWasDir = false
    if (file.isDirectory) {
      inputWasDir = true
      file = new File(file, "index.html")
    }
    
    // Find the dynamic alias information
    var dir = file.getParentFile
    while (!new File(dir,"dyn-alias.txt").exists && dir.getParentFile != null) {
      dir = dir.getParentFile
    }
    
    // Load the JavaScript code
    val loader = new Loader
    loader.addFile(file)
    val asts = loader.getAsts
    
    // Load dynamic aliasing information
    val dynfile = new File(dir, "dyn-alias.txt")
    val dyninfo = DynamicAliasInfo.load(Source.fromFile(dynfile), asts, dir)
    
    // A name is dead if its base set is empty
    def isDeadName(tok:AstNode) = dyninfo.objectsAt(NameRef.base(tok)).isEmpty
    
    // Compute mapping between tokens and their dynamic base objects
    val token2base = new mut.MultiMap2[String,AstNode,Int]
    val base2token = new mut.MultiMap2[String,Int,AstNode]
    asts.visitAll(new VisitAll {
      def handle(node:AstNode) {
        val base = NameRef.base(node)
        if (base == null)
          return
        val name = NameRef.name(node)
        for (obj <- dyninfo.objectsAt(base)) {
          token2base.add(name, node, obj)
          base2token.add(name, obj, node)
        }
      }
    })
    
    // Compute type info etc for static renaming
    val renaming = new Renaming(asts)
    
    // Compare static and dynamic renaming separately for each property name
    val names = token2base.keySet
    for (name <- names) {
      // Compute dynamic relatedness
      val dynequiv = new Equivalence[AstNode]
      for ((tok,obj) <- token2base.get(name); tok2 <- base2token(name,obj)) {
        dynequiv.unify(tok, tok2)
      }
      val dynrep = dynequiv.getResult()
      
      // Compute static relatedness
      val questions = renaming.renameProperty(name).
                      map(_.toList).toList // deep conversion to scala lists
      val staticrep = Equivalence.fromGroups(questions)
      
      // Check that static relatedness is a subset of dynamic relatedness
      val suspicious = Equivalence.nonSubsetItems(staticrep, dynrep)
      for ((x,y) <- suspicious) {
        val isDead = isDeadName(x) || isDeadName(y)
        var extraStr = if (isDead) "[dead]" else ""
        if (showdead || !isDead) {
          Console.printf("\"%s\" at %s:%d and %s:%d %s\n",
                         name,
                         asts.source(x),
                         1+asts.absoluteLineNo(x),
                         asts.source(y),
                         1+asts.absoluteLineNo(y),
                         extraStr)
        }
      }
    }
    
    Console.println("-" * 60)
    
    // Estimate coverage
    var numTokens = 0
    var numCoveredTokens = 0
    asts.visitAll(new VisitAll {
      def handle(node:AstNode) {
        if (NameRef.isPrty(node)) {
          numTokens += 1
          if (!dyninfo.objectsAt(NameRef.base(node)).isEmpty) {
            numCoveredTokens += 1
          }
        }
      }
    })
    val cov = EvalUtil.percent(numCoveredTokens, numTokens)
    Console.printf("Property name token coverage: %4.2f%%", cov);
  }
}