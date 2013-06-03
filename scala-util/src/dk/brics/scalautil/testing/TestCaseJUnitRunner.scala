package dk.brics.scalautil.testing

import java.io.{FileFilter, File}
import org.junit.runner.{Description, Runner}
import org.junit.runner.manipulation.{Filter, Filterable}
import org.junit.runner.notification.{Failure, StoppedByUserException, RunNotifier}
import java.util.Comparator
import java.util.Arrays

class TestCaseJUnitRunner(val executorClass: java.lang.Class[_]) extends Runner with Filterable {

  val testcaseDir = executorClass.newInstance().asInstanceOf[TestCaseDir].testcaseDir

  val filesInDir = testcaseDir.listFiles(new FileFilter {
      def accept(file:File) = file.isFile && file.getName.endsWith(".js")
    })
  Arrays.sort(filesInDir, FileNameComparator)
  var testCases = filesInDir.map(f => (f, Description.createTestDescription(executorClass, f.getName)))
  var junitSuite = createDescription

  def filter(fl: Filter) {
    testCases = testCases.filter({case (f,desc) => fl.shouldRun(desc)})
    junitSuite = createDescription
  }

  def createDescription: Description = {
    val suite = Description.createSuiteDescription(executorClass)
    for ( (file, desc) <- testCases) {
      suite.addChild(desc)
    }
    suite
  }

  def getDescription = junitSuite

  def run(notifier: RunNotifier) {
    val executor = executorClass.newInstance().asInstanceOf[TestExecutor]
    notifier.fireTestRunStarted(junitSuite)
    for ( (file,desc) <- testCases) {
      notifier.fireTestStarted(desc)
      try {
        executor.executeTest(file)
      } catch {
        case e:StoppedByUserException => return;
        case e:Throwable => notifier.fireTestFailure(new Failure(desc, e))
      }
      notifier.fireTestFinished(desc)
    }
  }
    
  object FileNameComparator extends Comparator[File] {
    def compare(f1: File, f2: File): Int = {
      f1.getName.compareTo(f2.getName)
    }
  }

}