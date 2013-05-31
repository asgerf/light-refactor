package dk.brics.scalautil.mut
import scala.collection.mutable.HashSet

import scala.collection._

object SetUtil {
  def addAll[A](dest: mutable.Set[A], source: TraversableOnce[_ <: A]): Boolean = {
    var changed = false;
    source.foreach(v => {changed |= dest.add(v)})
    changed
  }
  def copy[A](x: Set[A]): HashSet[A] = {
    val y = new HashSet[A]
    y ++= x
    y
  }
}
