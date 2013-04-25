package dk.brics.lightrefactor.util

import java.util.Scanner

abstract class Console {
  private static val Scanner scanner = new Scanner(System::in)
  def static readInt() {
    scanner.nextInt
  }
  def static readLine() {
    scanner.nextLine
  }
  def static promptInt(String msg) {
    var Integer answer = null
    while (answer === null) {
      println(msg)
      print("> ")
      val x = readLine
      try {
        answer = Integer::valueOf(x)
      } catch (NumberFormatException e) {
        println("Please answer with a number")
      }
    }
    return answer.intValue
  }
  def static promptString(String msg) {
    println(msg)
    print("> ")
    readLine()
  }
  def static promptYesNo(String msg) {
    var Boolean answer = null
    while (answer === null) {
      print(msg)
      println(" (Y/n)")
      print("> ")
      val txt = readLine.toLowerCase
      if (txt == "n" || txt == "no") {
        answer = false
      } else if (txt == "y" || txt == "yes" || txt == "") {
        answer = true
      } else {
        println("Please answer Y or N")
      }
    }
    return answer.booleanValue
  }
  def static err(String msg) {
    System::err.println(msg)
  }
}
