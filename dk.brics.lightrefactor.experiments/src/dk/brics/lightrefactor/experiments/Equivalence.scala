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

object Equivalence {
  /**
   * Computes representative map given a list of groupings.
   * If there are duplicates, the behaviour is undefined.
   */
  def fromGroups[T](groups:Traversable[Traversable[T]]) = {
    val map = new mutable.HashMap[T,T]
    for (list <- groups if !list.isEmpty) {
      val hd = list.head
      for (item <- list) {
        map += item -> hd
      }
    }
    map.toMap
  }
  
  /**
   * True if the equivalence relation imposed by the first map is a subset of the
   * relation imposed by the second map.
   */
  def isSubset[T](small:Map[T,T], large:Map[T,T]) : Boolean = {
    for ((k,v) <- small) {
      if (large(v) != large(k))
        return false;
    }
    true
  }
  
  /**
   * Like `isSubset`, but returns examples of pairs that are related in the first
   * argument, but not in the second.
   */
  def nonSubsetItems[T](small:Map[T,T], large:Map[T,T]) = {
    val examples = new mutable.ListBuffer[(T,T)]
    for ((k,v) <- small) {
      if (large(v) != large(k)) {
        examples += ((v,k))
      }
    }
    examples.toList
  }
}