package dk.brics.lightrefactor.experiments
import java.io.File
import java.io.FileWriter

import scala.collection.JavaConversions._
import scala.collection.mutable

import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.NodeVisitor
import org.mozilla.javascript.ast.ScriptNode

import dk.brics.lightrefactor.Asts
import dk.brics.lightrefactor.Loader
import dk.brics.lightrefactor.NameRef
import dk.brics.lightrefactor.Renaming


/**
 * Measures the number of questions asked by our tool, compared to search-and-replace.
 * Also analyzes each benchmark in isolation to measure how well the tool responds to missing
 * library/client code.
 */
object MeasureEffect {
  import EvalUtil._
  
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
  
  def printHelp() {
    Console.println("Usage: effect [DIR]")
    Console.println("Measures number of questions vs search-replace")
    Console.println("Also compares against benchmarks in isolation (i.e. without libs)")
    Console.println()
    Console.println("Output format:")
    Console.println("    benchmark-name effect [isolated delta]")
    Console.println("")
    Console.println("Passing the optional DIR argument focuses on only one benchmark")
  }
  
  def main(args:Array[String]) {
//    Console.printf("%-30s %6s %s\n", "#benchmark", "effect", "[isolated delta]")
    if (args.contains("-h")) {
      printHelp();
      System.exit(0)
    }
    val dirs =
      if (args.length == 0)
        benchmarkDirs(includeFake=false)
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
          val outfile = new File(outputDir, "isolated-" + name + ".txt")
          val writer = new FileWriter(outfile)
          try {
            for ((x,y) <- suspiciousPairs) {
              writer.write("\"" + NameRef.name(x) + "\" at " + asts.source(x) + ":" + (1+asts.absoluteLineNo(x)) + 
                           " and " + asts.source(y) + ":" + (1+asts.absoluteLineNo(y)))
              writer.write("\n")
            }
          } finally {
            writer.close()
          }
          suspiciousStr = "[potential failure: see " + pathTo(outfile) + "]"
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
      var sumWholeEffect = 0.0
      for ((whole,isolated) <- libStats(libkey)) {
        val delta = isolated.effect - whole.effect
        sumDelta += delta
        sumWholeEffect += whole.effect
      }
      val numSamples = libStats(libkey).size
      val avgDelta = sumDelta / numSamples
      val avgEffect = sumWholeEffect / numSamples
      val deltaStr = if (avgDelta == 0) "" else "[%6.2f]".format(avgDelta)
      Console.printf("%-30s %6.2f %s\n", libkey, avgEffect, deltaStr)
    }
    
    // Dump per-name stats
    val outfile = new File(outputDir, "namestats.txt")
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
    Console.println("Per-name stats written to " + pathTo(outfile))
    
  }
}