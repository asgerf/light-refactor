package dk.brics.lightrefactor.test

import java.util.ArrayList
import org.junit.Assert
import org.junit.Test

import static extension dk.brics.lightrefactor.util.MapExtensions.*

class Extensions {
  def <T>iterableEq(Iterable<T> xit, Iterable<T> yit) {
    val xs = new ArrayList<T>
    val ys = new ArrayList<T>
    for (x : xit) {
      xs.add(x)
    }
    for (y : yit) {
      ys.add(y)
    }
    Assert::assertEquals(xs, ys)
  }
  
  @Test
  def testConcat() {
    iterableEq(#[1, 2, 3].concat(#[4, 5]), #[1,2,3,4,5])
  }
  @Test
  def testConcatNested1() {
    iterableEq(#[1, 2, 3].concat(#[4, 5].concat(#[6,7])), #[1,2,3,4,5,6,7])
  }
  @Test
  def testConcatNested2() {
    iterableEq(#[1, 2, 3].concat(#[4, 5]).concat(#[6,7]), #[1,2,3,4,5,6,7])
  }
  @Test
  def testConcatNested3() {
    iterableEq((#[1, 2, 3].concat(#[6,7])).concat(#[4, 5]), #[1,2,3,6,7,4,5])
  }
}