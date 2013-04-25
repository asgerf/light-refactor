package dk.brics.lightrefactor.util

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.ArrayList

class IO {
  def static readFile(File file) {
    val sb = new StringBuilder
    val reader = new BufferedReader(new FileReader(file))
    try {
      var line = reader.readLine()
      while (line != null) {
        sb.append(line).append("\n")
        line = reader.readLine()
      }
    } finally {
      reader.close()
    }
    sb.toString
  }
  
  def static readLines(File file) {
    val list = new ArrayList<String>
    val reader = new BufferedReader(new FileReader(file))
    try {
      var line = reader.readLine()
      while (line != null) {
        list.add(line)
        line = reader.readLine()
      }
    } finally {
      reader.close()
    }
    list
  }
}
