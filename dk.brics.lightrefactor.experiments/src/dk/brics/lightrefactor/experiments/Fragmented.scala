package dk.brics.lightrefactor.experiments
import java.util.Random
import scala.collection.mutable
import org.mozilla.javascript.ast.AstNode
import org.mozilla.javascript.ast.AstRoot
import org.mozilla.javascript.ast.FunctionCall
import org.mozilla.javascript.ast.FunctionNode
import org.mozilla.javascript.ast.NodeVisitor
import org.mozilla.javascript.ast.ParenthesizedExpression
import dk.brics.lightrefactor.Loader
import java.io.File
import scala.collection.JavaConversions._
import dk.brics.lightrefactor.ISource
import dk.brics.lightrefactor.Asts
import dk.brics.lightrefactor.VisitAll
import dk.brics.lightrefactor.HtmlSource
import scala.io.Source
import dk.brics.scalautil.IO
import dk.brics.jshtml.Html
import dk.brics.jshtml.ExternJs
import dk.brics.jshtml.InlineJs
import org.mozilla.javascript.ast.ScriptNode
import dk.brics.lightrefactor.JsSource

class RandomizedFragmentor(ast:AstRoot) {
  
  val bodies = new mutable.ListBuffer[ScriptNode]
  val body2size = new mutable.HashMap[ScriptNode,Int]
  
  private def computeSize(node:ScriptNode) : Int = {
    var size = 0
    bodies += node
    node.visit(new NodeVisitor() {
      def visit(child:AstNode) : Boolean = {
        if (child == node)
          return true
        size += 1
        child match {
          case child:FunctionNode => size += computeSize(child); false
          case _ => true
        }
      }
    })
    body2size += node -> size
    size
  }
  computeSize(ast)
  
  def makeKillSet(sizeRatio:Double, random:Random) : Set[FunctionNode] = {
    val totalSize = body2size(ast)
    var remainingTokens = totalSize
    var needToRemove = totalSize * (1.0 - sizeRatio)
    def chance(prob:Double) = {
      random.nextDouble() < prob
    }
    def isOneShot(node:FunctionNode) = {
      var z : AstNode = node
      var p = node.getParent
      while (p.isInstanceOf[ParenthesizedExpression]) {
        z = p
        p = p.getParent
      }
      p match {
        case p:FunctionCall if p.getTarget == z => true
        case _ => false
      }
    }
    var result = Set.empty[FunctionNode]
    ast.visit(new NodeVisitor {
      def visit(node:AstNode) = {
        node match {
          case node:FunctionNode if !isOneShot(node) =>
            val sz = body2size(node)
            if (needToRemove > 0 && sz < totalSize/2) {
              val prob = needToRemove / remainingTokens.asInstanceOf[Double]
              remainingTokens -= sz
              if (chance(prob)) {
                needToRemove = math.max(0, needToRemove - sz)
                // delete function and all its inner functions
                result += node
                node.visit(new VisitAll {
                  def handle(node:AstNode) {
                    node match {
                      case node:FunctionNode => result += node
                      case _ =>
                    }
                  }
                })
                false // body cleared - don't visit inner function
              } else {
                true // body survived - visit inner functions
              }
            } else {
              true
            }
          case _ => true
        }
      }
    })
    result
  }
  
}

object Fragmented {
  
  def replaceWithWhitespace(code:String, sb:StringBuilder) {
    for (ch <- code) {
      if (Character.isWhitespace(ch) || ch == '\r' || ch == '\n') {
        sb.append(ch)
      } else {
        sb.append(' ')
      }
    }
  }
  
  def getFragmentedCode(code:String, killed:Set[FunctionNode]) = {
    val sb = new StringBuilder
    var offset = 0
    for (node <- killed.toList.sortBy(q => q.getAbsolutePosition)) {
      val pos = node.getBody.getAbsolutePosition
      if (pos >= offset) {
        sb.append(code.substring(offset, pos))
        sb.append("{ /* removed! */ ")
        replaceWithWhitespace(code.substring(pos, pos + node.getBody.getLength), sb)
        sb.append("}")
        offset = pos + node.getBody.getLength        
      }
    }
    val str = code.substring(offset)
    sb.append(code.substring(offset))
    sb.toString
  }
  
  def printFragmentedSource(dst:File, asts:Asts, killed:Set[FunctionNode]) {
    val files = (for (ast <- asts) yield asts.source(ast).getFile).toSet
    for (file <- files) {
      if (file.getName.endsWith(".html")) {
        // get all ASTs from this HTML file and sort them by starting offset
        val htmlFragments = Html.extract(file)
        IO.writeFile(new File(dst, file.getName + ".js")) { case writer =>
          htmlFragments foreach {
            case frag:InlineJs =>
              writer.write("// fragment from line " + frag.getLine + "\n")
              val src = new HtmlSource(file, frag.getOffset, frag.getLine, frag.getIndex)
              val ast = asts.get(src)
              writer.write(getFragmentedCode(frag.getCode, killed.filter(q => asts.source(q) == src)))
              writer.write("\n\n")
            case frag:ExternJs => // ignore
          }
        }
      } else {
        val code = IO.readStringFromFile(file)
        val src = new JsSource(file)
        IO.writeFile(new File(dst, file.getName)) { case writer =>
          writer.write(getFragmentedCode(code, killed.filter(q => asts.source(q) == src)))
        }
      } 
    }
  }
  
  def main(args:Array[String]) {
    val numTrials = 10
    val deletePercent = 0.50
    
    val outputDir = new File("output")
    
    val dirs = EvalUtil.benchmarkDirs(includeLibs=true)
    
    for (dir <- dirs) {
      val libs = EvalUtil.libraries(dir)
      val loader = new Loader
      if (new File(dir, "index.html").exists()) {
        loader.addFile(new File(dir, "index.html"))
      } else {
        // handle lib dirs
        for (js <- EvalUtil.allJavaScriptFiles(dir)) {
          loader.addFile(js)
        }
      }
      val asts = loader.getAsts
      
//      val completeStats = Precision.analyze(asts, q => true)
      
      def isFileInLib(x:File) = libs.values.exists(lib => x.getCanonicalPath.startsWith(lib.getCanonicalPath))
      
      val fragmenters = new mutable.HashMap[ISource,RandomizedFragmentor]
      for (ast <- asts if !isFileInLib(asts.source(ast).getFile)) {
        fragmenters += asts.source(ast) -> new RandomizedFragmentor(ast)
      }
      val sources = fragmenters.keys.toList.sortBy(_.toString) // list with deterministic iteration order
      var deltaSum = 0.0
      val randomSeed = dir.getName.hashCode() //+ 131 * src.toString.hashCode()
      val rand = new Random(randomSeed)
      for (trialNr <- 0 until numTrials) {
        var killedFragments = Set.empty[FunctionNode]
        for (src <- sources) {
          killedFragments ++= fragmenters(src).makeKillSet(1.0 - deletePercent, rand)
        }
        
        // print fragmented AST to file (for manual inspection)
        val outdir = new File(outputDir, dir.getName + "-frag" + trialNr)
        outdir.mkdirs()
        printFragmentedSource(outdir, asts, killedFragments)
        
        // analyze as whole and as fragmented and compare 
        def nodeSurvived(node:AstNode) = node.getEnclosingFunction == null || !killedFragments(node.getEnclosingFunction)
        val completeStats = Precision.analyze(asts, nodeSurvived)
        val fragmentedStats = Precision.analyzeFragmented(asts, nodeSurvived, killedFragments)
        
        val delta = fragmentedStats.effect - completeStats.effect
        val deltaStr = if (delta == 0) "" else "[%5.2f pp]".format(delta)
        Console.printf("%-20s %5d / %5d (%3.1f%%) vs %5d / %5d (%3.1f%%) %s\n", 
            dir.getName + "-" + trialNr, 
            fragmentedStats.renameQuestions,
            fragmentedStats.searchReplaceQuestions,
            fragmentedStats.effect,
            completeStats.renameQuestions,
            completeStats.searchReplaceQuestions,
            completeStats.effect,
            deltaStr)
      }
    }
  }
}