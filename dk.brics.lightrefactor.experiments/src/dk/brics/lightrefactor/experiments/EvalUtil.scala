package dk.brics.lightrefactor.experiments
import java.io.File
import scala.collection.mutable
import scala.io.Source

object EvalUtil {
  val benchmarksDir = new File("benchmarks")
  
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
}