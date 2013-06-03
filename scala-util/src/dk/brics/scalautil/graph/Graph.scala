package dk.brics.scalautil.graph

import scala.collection.mutable
import dk.brics.scalautil.immut.MultiMap
import scala.collection.mutable
import scala.annotation.tailrec

object Graph {
  def reachable[V](roots:Set[V], succ:V=>Traversable[V]) : Set[V] = {
    val queue = new mutable.Queue[V]
    val result = new mutable.HashSet[V]
    queue ++= roots
    result ++= roots
    while (!queue.isEmpty) {
      val v = queue.dequeue()
      for (w <- succ(v)) {
        if (result.add(w)) {
          queue += w
        }
      }
    }
    result.toSet
  }
  
  /**
   * Computes the strongly connected components of the given graph.
   * Returns a map such that every (non-isolated) vertex maps to
   * the representative of its component.
   */
  def strongComponents[V](edges:MultiMap[V,V]) : Map[V,V] = {
    var rep = Map.empty[V,V]
    
    var index = 0
    val stack = new mutable.Stack[V]
    val onstack = new mutable.HashSet[V]
    val vindex = new mutable.HashMap[V,Int]
    val vlowlink = new mutable.HashMap[V,Int]
    
    def strongConnect(v:V) {
      vindex += v -> index
      vlowlink += v -> index
      index += 1
      stack.push(v)
      onstack += v
      
      for (w <- edges(v)) {
        vindex.get(w) match {
          case None =>
            strongConnect(w)
            vlowlink += v -> math.min(vlowlink(v), vlowlink(w))
          case Some(widx) =>
            if (onstack.contains(w)) {
              vlowlink += v -> math.min(vlowlink(v), vindex(w))
            }
        }
      }
      
      if (vlowlink(v) == vindex(v)) {
        // 'v' is now the representative for its SCC
        @tailrec def unfoldStack() {
          val w = stack.pop()
          onstack -= w
          rep += w -> v
          if (v != w) {
            unfoldStack()
          }
        }
        unfoldStack()
      }
    }
    
    rep
  }
  
  /**
   * Computes edges between components in a graph.
   * @param edges the graph's edge relation
   * @param rep maps a vertex to its component's representative
   * @return relation on representatives 
   */
  def componentEdges[V,R](edges:MultiMap[V,V], rep:Map[V,R]) : MultiMap[R,R] = {
    var result = MultiMap.empty[R,R]
    for ((v,w) <- edges) {
      val rv = rep(v)
      val rw = rep(w)
      if (rv != rw) {
        result += rv -> rw
      }
    }
    result
  }
  
  def simpleReachability[V](edges:MultiMap[V,V], sinks:V=>Boolean) {
    
  }
}
