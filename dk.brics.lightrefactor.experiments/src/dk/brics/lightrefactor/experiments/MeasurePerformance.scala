package dk.brics.lightrefactor.experiments
import dk.brics.lightrefactor.Loader
import java.io.File
import dk.brics.lightrefactor.types.TypeInference
import scala.collection.mutable
import dk.brics.lightrefactor.Asts
import dk.brics.lightrefactor.VisitAll
import org.mozilla.javascript.ast.AstNode
import dk.brics.lightrefactor.NameRef
import dk.brics.lightrefactor.Renaming

/**
 * Measures performance while simulating a renaming on each benchmark.
 */
object MeasurePerformance {
  def findMostFrequentPropertyName(asts:Asts) = {
    val freq = new mutable.HashMap[String,Int]
    asts.visitAll(new VisitAll {
      def handle(node:AstNode) {
        if (NameRef.isPrty(node)) {
          val name = NameRef.name(node)
          freq += name -> (1 + freq.getOrElse(name,0))
        }
      }
    })
    val (name,_) = freq.maxBy{case (name,count) => count}
    name
  }
  
  def printHelp() {
    Console.println("Usage: performance [-h]")
    Console.println("Measures the number of seconds it takes to rename the most frequently")
    Console.println("occuring property name for each application. Timings are averaged over ten runs.")
    Console.println("Analysis includes library code.")
    Console.println()
    Console.println("Output format:")
    Console.println("    benchmark-name seconds")
  }
  
  import EvalUtil._
  def main(args:Array[String]) {
    if (args.contains("-h")) {
      printHelp()
      System.exit(0)
    }
    val dirs = benchmarkDirs()
    for (dir <- dirs) {
      // Parse the input
      val startParse = System.nanoTime()
        val loader = new Loader
        loader.addFile(new File(dir, "index.html"))
        val asts = loader.getAsts
      val parseTimeNano = System.nanoTime()- startParse
      
      // Use the most frequent property name as the candidate for renaming
      val name = findMostFrequentPropertyName(asts)
      
      // warm-up run
      new Renaming(asts).renameProperty(name)
      
      // run 10 times
      val startTrials = System.nanoTime()
      val numTrials = 10
      for (i <- 0 until numTrials) {
        new Renaming(asts).renameProperty(name)
      }
      val avgTrialTimeNano = (System.nanoTime() - startTrials) / numTrials
      
      // print the average time
      Console.printf("%-20s %2.2f\n", dir.getName, avgTrialTimeNano / 1000000000.0f)
    }
  }
}