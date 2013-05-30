package dk.brics.lightrefactor.experiments
import java.io.File
import scala.collection.mutable
import java.io.FileWriter
import dk.brics.lightrefactor.Asts
import dk.brics.lightrefactor.NameRef
import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.StringLiteral
import org.mozilla.javascript.ast.NodeVisitor
import dk.brics.lightrefactor.VisitAll
import dk.brics.lightrefactor.Renaming
import dk.brics.lightrefactor.Loader
import scala.collection.JavaConversions._
import dk.brics.lightrefactor.util.IO
import org.mozilla.javascript.ast.ScriptNode
import org.mozilla.javascript.ast.FunctionNode


/**
 * Measures the number of questions asked by our tool, compared to search-and-replace.
 */
object Precision {
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
    NameRef.isPrty(ref) && NameRef.name(ref) != "prototype" //&& !ref.isInstanceOf[StringLiteral]
  
  def analyze(asts:Asts, pred:AstNode => Boolean) = analyzeFragmented(asts, pred, Set.empty)
      
  def analyzeFragmented(asts:Asts, pred:AstNode => Boolean, killed:Set[FunctionNode]) = {
    // collect all property name tokens
    val nameRefs = new mutable.ListBuffer[AstNode]
    asts.visitAll(new VisitAll {
      override def handle(node:AstNode) {
        if (NameRef.isPrty(node) && includeRef(node) && pred(node)) {
          nameRefs += node
        }
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
  
  trait Input {
    val libs : Map[String,File]
    val asts : Asts
  }
  
  def getInput(file:File) = {
    if (file.isFile) {
      val loader = new Loader
      loader.addFile(file)
      new Input {
        val libs = Map.empty[String,File]
        val asts = loader.getAsts
      }
    } else {
      val loader = new Loader
      loader.addFile(new File(file, "index.html"))
      new Input {
        val libs = EvalUtil.libraries(file)
        val asts = loader.getAsts
      }
    }
  }
  
  def main(args:Array[String]) {
//    Console.printf("%-30s %6s %s\n", "#benchmark", "effect", "[isolated delta]")
    val dirs =
      if (args.length == 0)
        EvalUtil.benchmarkDirs(includeFake=false)
      else
        List(new File(args(0)))
    
    val libStats = new mutable.HashMap[String, mutable.ListBuffer[(Stats,Stats)]]
    
    def addLibStats(name:String, appLibStats:Stats, libAloneStats:Stats) {
      libStats.getOrElseUpdate(name, new mutable.ListBuffer) += ((appLibStats, libAloneStats))
    }
    
    val totalNameStats = new mutable.ListBuffer[(String,NameStats)]
    
    for (dir <- dirs) {
      val input = getInput(dir)
      val libs = input.libs
      val asts = input.asts
      
      def isFileInLib(x:File) : Boolean = {
        libs.values.exists(lib => x.getCanonicalPath.startsWith(lib.getCanonicalPath))
      }
      def isRefInLib(x:AstNode) = {
        isFileInLib(asts.source(x).getFile)
      }
      
      def printRelativeStats(name:String, whole:Stats, isolated:Stats) {
        val delta = isolated.effect - whole.effect
        val deltaStr = if (delta == 0) "" else "[%6.2f]".format(delta)
        
        val suspiciousPairs = new mutable.ListBuffer[(AstNode,AstNode)]
        for (wholeSt <- whole.nameStats) {
          isolated.name2stats.get(wholeSt.name) match {
            case None =>
            case Some(isolatedSt) =>
              suspiciousPairs ++= Equivalence.nonSubsetItems(isolatedSt.equiv, wholeSt.equiv)
          }
        }
        var suspiciousStr = ""
        if (suspiciousPairs.size > 0) {
          val outfile = "output/suspicious-" + name + ".txt"
          val writer = new FileWriter(new File(outfile))
          try {
            for ((x,y) <- suspiciousPairs) {
              writer.write("\"" + NameRef.name(x) + "\" at " + asts.source(x) + ":" + (1+asts.absoluteLineNo(x)) + 
                           " and " + asts.source(y) + ":" + (1+asts.absoluteLineNo(y)))
              writer.write("\n")
            }
          } finally {
            writer.close()
          }
          suspiciousStr = "[potential failure: see " + outfile + "]"
        }
        
        Console.printf("%-30s %6.2f %s %s\n", name, whole.effect, deltaStr, suspiciousStr)
      }
      
      {
        // analyze app together with libs (but count only renamings inside the app itself)
        val appLibStats = analyze(asts, x => !isRefInLib(x))
        
        // analyze app without libs
        val appAsts = new Asts
        for (ast <- asts) {
          val src = asts.source(ast)
          if (!isFileInLib(src.getFile)) {
            appAsts.add(src, ast)
          }
        }
        val appNoLibStats = analyze(appAsts, x => true)
        printRelativeStats(dir.getName, appLibStats, appNoLibStats)
        
        totalNameStats ++= appLibStats.nameStats.map((dir.getName,_))
      }
      
      // compute stats for each lib used by this app
      for ((key,lib) <- libs) {
        val libAsts = new Asts
        for (ast <- asts) {
          val src = asts.source(ast)
          if (src.getFile.getCanonicalPath.startsWith(lib.getCanonicalPath)) {
            libAsts.add(src, ast)
          }
        }
        // analyze library standalone
        val libStats = analyze(libAsts, x => true)
        
        // analyze library with entire app, but only count renamings inside the library
        val appLibStats = analyze(asts, x => asts.source(x).getFile.getCanonicalPath.startsWith(lib.getCanonicalPath))
        
        addLibStats(key, appLibStats, libStats)
      }
    }
    
    Console.println("--------------- LIBRARIES")
    
    // Compute averaged values for each lib (may vary due to versioning or different application code)
    for (libkey <- libStats.keys.toList.sorted) {
      var sumDelta = 0.0
      var sumWholeEffort = 0.0
      for ((whole,isolated) <- libStats(libkey)) {
        val delta = isolated.effect - whole.effect
        sumDelta += delta
        sumWholeEffort += whole.effect
      }
      val numSamples = libStats(libkey).size
      val avgDelta = sumDelta / numSamples
      val avgEffort = sumWholeEffort / numSamples
      val deltaStr = if (avgDelta == 0) "" else "[%6.2f]".format(avgDelta)
      Console.printf("%-30s %6.2f %s\n", libkey, avgEffort, deltaStr)
    }
    
    // Dump per-name stats
    val outfile = new File("output/namestats.txt")
    outfile.getParentFile.mkdirs()
    val writer = new FileWriter(outfile)
    try {
      for ((benchmark,stat) <- totalNameStats.toList.sortBy(q => q._1 + " " + q._2.name)) {
        writer.write("%s %s %d %d\n".format(benchmark, stat.name, stat.searchReplaceQuestions, stat.renameQuestions))
      }
    } finally {
      writer.close()
    }
    Console.println("---------------")
    Console.println("Per-name stats written to " + outfile.getPath)
    
  }
}