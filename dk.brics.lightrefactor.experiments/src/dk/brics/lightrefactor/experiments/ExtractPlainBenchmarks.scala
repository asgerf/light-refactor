package dk.brics.lightrefactor.experiments
import java.io.File
import java.io.FileWriter
import scala.io.Source
import dk.brics.jshtml.Html
import scala.collection.JavaConversions._
import dk.brics.jshtml.InlineJs
import dk.brics.jshtml.ExternJs

/**
 * Creates a single JavaScript file for each benchmark with all the JavaScript code in it.
 * Useful for feeding the benchmark into other tools that cannot handle HTML.
 */
object ExtractPlainBenchmarks {
  def main(args:Array[String]) {
    val outdir = new File("output/plain-benchmarks")
    outdir.mkdirs()
    val dirs = EvalUtil.benchmarkDirs()
    for (dir <- dirs) {
      val targetDir = new File(outdir, dir.getName)
      targetDir.mkdir()
      
      val targetFile = new File(targetDir, "benchmark.js")
      val writer = new FileWriter(targetFile)
      
      try {  
        def dumpFile(name:String, code:String) {
          writer.write("// FILE: ")
          writer.write(name)
          writer.write("\n")
          writer.write(code)
          writer.write("\n\n")
        }
        val fragments = Html.extract(new File(dir, "index.html"))
        for (frag <- fragments) {
          frag match {
            case frag:InlineJs =>
              dumpFile("chunk" + frag.getLine + ".js", frag.getCode)
            case frag:ExternJs =>
            val file = new File(dir, frag.getURL)
            dumpFile(frag.getURL, Source.fromFile(file).getLines.mkString("\n"))
          }
        }
      }
      finally {
        writer.close()
      }
      Console.println(dir.getName)
    }
  }
}