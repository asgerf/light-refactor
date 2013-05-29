package dk.brics.lightrefactor.experiments
import java.io.File
import java.io.FileWriter

/**
 * Modifies the libs.txt file of a benchmark program to indicate
 * that the given file is part of a library.
 */
object MarkAsLibrary {
  def main(args:Array[String]) {
    val file = new File(args(0))
    
    var dir = file.getParentFile
    while (!new File(dir, "dyn-alias.txt").exists()) {
      dir = dir.getParentFile
      if (dir == null) {
        Console.err.println("File not part of benchmark: " + file)
        System.exit(1)
      }
    }
    val relative = EvalUtil.getRelative(dir, file)
    
    val writer = new FileWriter(new File(dir, "libs.txt"), true)
    try {
      writer.write(relative)
      writer.write("\n")
    } finally {
      writer.close()
    }
    
    Console.println("Marked " + relative + " as a library in " + dir.getName)
  }
}