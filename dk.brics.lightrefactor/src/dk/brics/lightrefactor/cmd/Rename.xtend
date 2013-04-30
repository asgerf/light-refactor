package dk.brics.lightrefactor.cmd

import dk.brics.lightrefactor.Asts
import dk.brics.lightrefactor.GenericSource
import dk.brics.lightrefactor.Renaming
import dk.brics.lightrefactor.types.TypeNode
import dk.brics.lightrefactor.util.Console
import dk.brics.lightrefactor.util.IO
import java.io.File
import java.io.PrintWriter
import java.util.ArrayList
import java.util.HashMap
import org.mozilla.javascript.Parser
import org.mozilla.javascript.ast.Name

import static extension dk.brics.lightrefactor.NameRef.*
import static extension dk.brics.lightrefactor.util.MapExtensions.*

@Data class GroupKey {
  TypeNode type
  String name
}

/**
 * Command-line based renaming tool.
 */
class Rename {
  
  def static void main(String[] args) {
    val file = new File(args.get(0))
    val sourceLines = IO::readLines(file)
    val str = sourceLines.join("\n")
    extension val offsets = new Offsets(str)
    val ast = new Parser().parse(str, file.getPath, 0)
    
    
    // collect all property name tokens
    val names = new ArrayList<Name>
    ast.visit [ node | switch node { Name case node.isPrty: names.add(node) }; true ]
    
    // determine the token to rename
    val linenr = Console::promptInt("On what line is the token you wish to rename?")
    val namesOnLine = names.filter[it.lineno+1 == linenr].toList
    var Name selectedToken
    if (namesOnLine.empty) {
      Console::err("There is no name token on line " + linenr)
      System::exit(1)
    } else if (namesOnLine.size == 1) {
      selectedToken = namesOnLine.get(0)
    } else {
      // print the selected line
      val srcline = sourceLines.get(linenr-1)
      println(srcline)
      
      // print ^^^ markers underneath
      var prevOffset = 0
      var index = 1
      for (name : namesOnLine) {
        val pos = name.linepos
        for (i : prevOffset..<pos) {
          if (srcline.charAt(i) == '\t'.charAt(0)) {
            print('\t')
          } else {
            print(' ')
          }
        }
        print (index.toString)
        for (i : index.toString.length..<name.string.length) {
          print('^')
        }
        index = index + 1
        prevOffset = pos + name.string.length
      }
      println()
      val selectedIndex = Console::promptInt("Which token would you like to rename?")
      selectedToken = namesOnLine.get(selectedIndex-1)
    }
    
    // determine new name
    val originalName = selectedToken.string
    val newName = Console::promptString('''Enter new name for token `«originalName»`:''')
    
    val asts = new Asts
    asts.add(new GenericSource, ast)
    val renaming = new Renaming(asts, selectedToken)
    
    val line2substitutions = new HashMap<Integer, ArrayList<Integer>> // linenr -> line positions to replace
    for (quest : renaming.questions) {
      var shouldRename = false
      if (quest.contains(selectedToken)) {
        shouldRename = true
      } else {
        // ask question
        for (name : quest) {
          // print line containing the name
          val lineno = (name.lineno + 1) + ". "
          val srcline = sourceLines.get(name.lineno)
          print(lineno)
          println(srcline)
          // print ^^^ markers
          for (i : 0..<lineno.length) {
            print(' ')
          }
          for (i : 0..<name.linepos) {
            if (srcline.charAt(i) == '\t'.charAt(0)) {
              print('\t')
            } else {
              print(' ')
            }
          }
          for (i : 0..<name.string.length) {
            print('^')
          }
          println()
        }
        shouldRename = Console::promptYesNo("Rename tokens on the above lines?")
      }
      if (shouldRename) {
        for (name : quest) {
          line2substitutions.getList(name.lineno).add(name.linepos)
        }
      }
    }
    
    println("-------------------------")
    // build the new source code
    val output = new PrintWriter(System::out)
    for (i : 0..<sourceLines.size) {
      val line = sourceLines.get(i)
      var prev = 0
      for (pos : line2substitutions.tryList(i)) {
        output.print(line.substring(prev,pos-prev))
        output.print(newName)
        prev = pos + originalName.length
      }
      output.print(line.substring(prev))
      output.println()
    }
    output.flush()
  }
}