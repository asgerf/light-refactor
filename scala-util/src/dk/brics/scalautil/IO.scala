package dk.brics.scalautil

import java.io._
import java.lang.StringBuilder

object IO {
  def writeStringToFile(file: File, contents: String) {
    file.getAbsoluteFile.getParentFile.mkdirs();
    val writer = new BufferedWriter(new FileWriter(file))
    try {
      writer.write(contents)
    } finally {
      writer.close()
    }
  }

  def readStringFromFile(file: File): String = {
    val reader = new BufferedReader(new FileReader(file))
    val sb = new StringBuilder
    try {
      var ch = reader.read()
      while (ch != -1) {
        sb.append(ch.asInstanceOf[Char])
        ch = reader.read()
      }
    } finally {
      reader.close()
    }
    sb.toString
  }

  def readStringListFromFile(file: File): List[String] = {
    val reader = new BufferedReader(new FileReader(file))
    var list = List.empty[String]
    try {
      var line = reader.readLine()
      while (line != null) {
        list ::= line
        line = reader.readLine()
      }
    } finally {
      reader.close()
    }
    list.reverse
  }
  
  def writeFile(file:File)(action : FileWriter=>Unit) {
    val writer = new FileWriter(file)
    try {
      action(writer)
    } finally {
      writer.close()
    }
  }
}
