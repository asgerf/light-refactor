package dk.brics.scalautil.testing

import java.io.File

trait TestExecutor {
  def executeTest(file: File)
}
