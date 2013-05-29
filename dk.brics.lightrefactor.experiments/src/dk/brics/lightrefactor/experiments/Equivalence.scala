package dk.brics.lightrefactor.experiments

import scala.collection.mutable

final class EquivNode[T](val item:T) {
  var parent = this
  var rank = 0
  
  def rep : EquivNode[T] = {
    if (parent == this) {
      this
    } else {
      val z = parent.rep
      parent = z
      z
    }
  }
}

/**
 * Computes an equivalence relation over T.
 */
class Equivalence[T] {
  val item2node = new mutable.HashMap[T,EquivNode[T]]
  private def getNode(x:T) = item2node.getOrElseUpdate(x, new EquivNode(x))
  def unify(x:T, y:T) {
    val xr = getNode(x).rep
    val yr = getNode(y).rep
    if (xr != yr) {
      val (high,low) = if (xr.rank > yr.rank) (xr,yr) else (yr,xr)
      low.parent = high
      if (high.rank == low.rank) {
        high.rank += 1
      }
    }
  }
  
  def getResult() : Map[T,T] = {
    var r = Map.empty[T,T]
    for ((t,node) <- item2node) {
      r += t -> node.rep.item
    }
    r
  }
}