package dk.brics.lightrefactor

import dk.brics.jshtml.ExternJs
import dk.brics.jshtml.Html
import dk.brics.jshtml.InlineJs
import java.io.File
import java.io.FileReader
import java.io.Reader
import org.mozilla.javascript.CompilerEnvirons
import org.mozilla.javascript.ErrorReporter
import org.mozilla.javascript.EvaluatorException
import org.mozilla.javascript.Parser

class SilentErrorHandler implements ErrorReporter {
  override def void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
  }
  override def void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
  }
  override def EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
    new CompilerEnvirons().errorReporter.runtimeError(message, sourceName, line, lineSource, lineOffset)
  }
}

/**
 * Loads JavaScript code from JS files and HTML files.
 * Example usage (Xtend syntax):
 * <pre>
 * val loader = new Loader
 * loader.addFile("index.html")
 * val asts = loader.asts
 * </pre>
 * <p/>
 * The parser settings can be changed by modifying or replacing the Loader's compilerEnv.
 * By default, the loader will configure the parser to be as forgiving as possible.
 */
class Loader {
  @Property var compilerEnv = new CompilerEnvirons
  @Property val asts = new Asts
  
  new() {
    compilerEnv.recoverFromErrors = true
    compilerEnv.ideMode = true
    compilerEnv.errorReporter = new SilentErrorHandler
  }
  
  def addSource(ISource src, String code) {
    val path = if (src.file != null) src.file.getPath else ""
    val ast = new Parser(compilerEnv).parse(code, path, 0)
    asts.add(src, ast)
  }
  def addSource(ISource src, Reader code) {
    val path = if (src.file != null) src.file.getPath else ""
    val ast = new Parser(compilerEnv).parse(code, path, 0)
    asts.add(src, ast)
  }
  def addSource(String code) {
    val ast = new Parser(compilerEnv).parse(code, "", 0)
    asts.add(new GenericSource(), ast)
  }
  
  /** 
   * Loads the given file and adds all JavaScript fragments to this Ast collection.
   * <p/>
   * Based on the file's extension, this will guess if it is a JavaScript file or an HTML file.
   * For HTML files, all fragments are loaded as separate ASTs.
   */
  def addFile(File file) {
    val name = file.getName.toLowerCase
    if (name.endsWith(".html") || name.endsWith(".htm")) {
      val frags = Html::extract(file);
      for (frag : frags) {
        switch frag {
          InlineJs: {
            val ast = new Parser(compilerEnv).parse(frag.code, file.getPath, 0)
            asts.add(new HtmlSource(file, frag.offset, frag.line, frag.index), ast)
          }
          ExternJs: {
            val basedir = file.getParent
            val target = new File(basedir, frag.URL)
            val ast = new Parser(compilerEnv).parse(new FileReader(target), target.getPath, 0)
            asts.add(new JsSource(target), ast)
          }
        }
      }
    } else if (name.endsWith(".js")) {
      val ast = new Parser(compilerEnv).parse(new FileReader(file), file.getPath, 0)
      asts.add(new JsSource(file), ast)
    } else {
      throw new IllegalArgumentException("Extension not recognized on file name: " + file)
    }
  }
}