package dk.brics.lightrefactor.cmd

import dk.brics.lightrefactor.Asts
import dk.brics.lightrefactor.Loader
import dk.brics.lightrefactor.types.TypeInference
import dk.brics.lightrefactor.types.TypeNode
import dk.brics.lightrefactor.types.Typing
import java.io.File
import org.mozilla.javascript.ast.VariableInitializer

class PrintTypes {
  extension val Asts asts
  extension val Typing types
  
  new (Asts asts, Typing types) {
    this.asts = asts
    this.types = types
  }
  
  def String formatType(TypeNode type) {
    "{" + type.properties.sort.filter[!startsWith("@")].join(",") + "}"
  }
  
  def void printTypes() {  
    asts.visitAll [ node |
      switch node {
        VariableInitializer: {
          println(node.target.source + ":" + (node.target.absoluteLineNo+1).toString + " " + node.target.string + " :: " + formatType(node.target.typ))
        }
      }
    ]
  }
  
  static def void main(String[] args) {
    val loader = new Loader
    for (arg : args) {
      if (arg.startsWith("-")) {
        System::err.println("No such option: " + arg)
        System::exit(1)
      }
      else {
        loader.addFile(new File(arg))
      }
    }
    val asts = loader.asts
    val types = new TypeInference(asts).inferTypes()
    val printer = new PrintTypes(asts,types)
    printer.printTypes()
  }
}