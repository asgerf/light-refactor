package dk.brics.scalautil

import immut._

/** A lattice with member type `T`. */
trait Lattice[T] {
  /** 
   * Returns the least upper bound of (`t1`,`t2`), together with a boolean indicating
   * if the least upper bound differs from `t1`.
   */
  def leastUpperBound(t1:T, t2:T): (T,Boolean)
  
  /** True if `t` is the bottom element */
  def isBottom(t:T): Boolean
  
  /** A bottom element */
  def bottom:T
}

object Lattice {
  /** Creates a lattice of maps, using the given lattice to merge values */
  implicit def map[K,V](implicit vl:Lattice[V]): Lattice[Map[K,V]] = new Lattice[Map[K,V]] {
    def leastUpperBound(t1:Map[K,V], t2:Map[K,V]) = {
      CollectionUtil.unionMaps(t1, t2, vl.leastUpperBound _)
    }
    def isBottom(m:Map[K,V]) = m.isEmpty
    def bottom = Map.empty
  }
  
  implicit def set[T] : Lattice[Set[T]] = new Lattice[Set[T]] {
    def leastUpperBound(t1:Set[T], t2:Set[T]) = {
      val result = t1 ++ t2
      (result, result.size > t1.size)
    }
    def isBottom(t:Set[T]) = t.isEmpty
    def bottom = Set.empty
  }
  
  implicit def multimap[K,V] : Lattice[MultiMap[K,V]] = new Lattice[MultiMap[K,V]] {
    def leastUpperBound(t1:MultiMap[K,V], t2:MultiMap[K,V]) = {
      t1.addAll(t2)
    }
    def isBottom(t:MultiMap[K,V]) = t.isEmpty
    def bottom = MultiMap.empty
  }
  
  implicit def multimap2[K,L,V] : Lattice[MultiMap2[K,L,V]] = new Lattice[MultiMap2[K,L,V]] {
    def leastUpperBound(t1:MultiMap2[K,L,V], t2:MultiMap2[K,L,V]) = {
      t1.addAll(t2)
    }
    def isBottom(t:MultiMap2[K,L,V]) = t.isEmpty
    def bottom = MultiMap2.empty
  }
}
