package dk.brics.lightrefactor.experiments
import java.io.File
import scala.collection.mutable
import scala.io.Source
import dk.brics.lightrefactor.NameRef
import dk.brics.lightrefactor.Renaming
import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.ScriptNode
import dk.brics.lightrefactor.Asts
import org.mozilla.javascript.ast.NodeVisitor
import scala.collection.JavaConversions._

object EvalUtil {
  val workdir = {
    if (System.getProperty("workdir") != null) {
      new File(System.getProperty("workdir"))
    } else {
      new File(".")
    }
  }
  val benchmarksDir = new File(workdir,"benchmarks")
  val outputDir = new File(workdir,"output")
  
  def pathTo(file:File) = getRelative(workdir, file)
  
  def benchmarkDirs(includeFake:Boolean=false, includeLibs:Boolean=false) : mutable.ListBuffer[File] = {
    // collect all benchmark directories into a list
    val dirs = new mutable.ListBuffer[File]
    for (category <- benchmarksDir.listFiles) {
      if (category.isDirectory && !category.getName.startsWith(".") 
          && (includeFake || category.getName != "fake")
          && (includeLibs || category.getName != "libs")) {
        for (dir <- category.listFiles) {
          if (dir.isDirectory && !dir.getName.startsWith(".")) {
            dirs += dir
          }
        }
      }
    }
    dirs.sortBy(_.getCanonicalPath)
  }
  
  def libraries(dir:File) : Map[String,File] = {
    val libtxt = new File(dir, "libs.txt")
    if (libtxt.exists()) {
      var result = Map.empty[String, File]
      for (line <- Source.fromFile(libtxt).getLines if line != "") {
        val List(key,file) = line.split(", *",2).toList
        result += key -> new File(dir, file)
      }
      result
    } else {
      Map.empty
    }
  }
  
  /** Path to `child` relative to the `base` dir. `child` must be contained in `base` */
  def getRelative(base:File, child:File) : String = {
    val baseStr = base.getCanonicalPath
    val childStr = child.getCanonicalPath
    if (!childStr.startsWith(baseStr))
      throw new RuntimeException("Not contained in base directory")
    else
      childStr.substring(baseStr.length+1)
  }
  
  def allJavaScriptFiles(dir:File) : List[File] = {
    val result = new mutable.ListBuffer[File]
    def visit(file:File) {
      if (file.isDirectory && !file.getName.startsWith(".")) {
        for (child <- file.listFiles) {
          visit(child)
        }
      }
      if (file.isFile && file.getName.endsWith(".js")) {
        result += file
      }
    }
    visit(dir)
    result.toList
  }
  
  
  def percent(x:Double, y:Double) = {
    if (y == 0)
      0
    else
      100 * (x / y)
  }
  def reduction(x:Double, y:Double) = {
    if (x == 0)
      0
    else
      y / x
  }
  
  case class NameStats(name:String, questions:List[List[AstNode]]) {
    lazy val equiv = Equivalence.fromGroups(questions)
    lazy val numTokens = questions.map(_.size).sum
    def searchReplaceQuestions = numTokens - 1
    def renameQuestions = questions.size - 1
    def effect = percent(searchReplaceQuestions - renameQuestions, searchReplaceQuestions)
  }
  case class Stats(nameStats:List[NameStats]) {
    lazy val name2stats = nameStats.map(q => (q.name,q)).toMap
    lazy val searchReplaceQuestions = nameStats.map(_.searchReplaceQuestions).sum
    lazy val renameQuestions = nameStats.map(_.renameQuestions).sum
    def effect = percent(searchReplaceQuestions - renameQuestions, searchReplaceQuestions)
  }
  
  /** Only consider name refs to properties, 
   *  and don't consider "prototype" properties because nobody ever tries to rename that. */ 
  def includeRef(ref:AstNode) =
    NameRef.isPrty(ref) && NameRef.name(ref) != "prototype"
  
  def analyze(asts:Asts, pred:AstNode => Boolean) = analyzeFragmented(asts, pred, Set.empty)
      
  def analyzeFragmented(asts:Asts, pred:AstNode => Boolean, killed:Set[ScriptNode]) = {
    // collect all property name tokens
    val nameRefs = new mutable.ListBuffer[AstNode]
    asts.visit(new NodeVisitor {
      override def visit(node:AstNode) = {
        if (NameRef.isPrty(node) && includeRef(node) && pred(node)) {
          nameRefs += node
        }
        if (killed.contains(node))
          false
        else
          true
      }
    });
    
    val count = new mutable.HashMap[String,Int]
    
    // count number of occurrences of each name
    for (ref <- nameRefs) {
      count += NameRef.name(ref) -> (1 + count.getOrElse(NameRef.name(ref),0))
    }
    
    val renaming = new Renaming(asts)
    renaming.ignoreNodes(killed)
    val nameStats = (for (name <- count.keys if count(name) > 1) yield {
      val questions = renaming.renameProperty(name).
          map(_.filter(q => includeRef(q) && pred(q)).toList). // filter out ignored tokens
          filter(!_.isEmpty).                                  // ignore questions that are now empty
          sortBy(q => asts.source(q.head) + ":" + q.head.getAbsolutePosition).
          toList
      NameStats(name, questions)
    }).toList
    
    Stats(nameStats)
  }
  
  
  
  val BIG = 1000000
  
  case class Range(from:Int, to:Int) { // inclusive, exclusive
    override def toString = {
      if (from+1 == to) {
        "" + from
      } else if (to == BIG) {
        from + "+"
      } else {
        from + ".." + (to-1)
      }
    }
  } 
  
  def parseRange(str:String) = {
    if (str.contains("..")) {
      val toks = str.split("\\.\\.")
      Range(toks(0).toInt, toks(1).toInt+1)
    }
    else if (str.endsWith("+")) {
      val num = str.substring(0, str.length-1)
      Range(num.toInt, BIG)
    }
    else {
      val z = str.toInt
      Range(z, z+1)
    }
  }
}