package dk.brics.scalautil.immut

/**
 * A relation type K -> L -> V, or equivalently, (K,L) -> V.
 * Map2[K,L,V] is closely related to the type Map[K,Map[L,V]], but with some differences:
 * 
 * In Map[K,Map[L,V]] a key of type K can be
 * - absent
 * - present and map to null
 * - present and map to an empty map
 * - present and map to a non-empty map
 * 
 * In Map2[K,L,V] a key K can be
 * - absent because it maps to an empty map
 * - present because it maps to a non-empty map
 * 
 * Note that the invariant for Map2 can be broken if the Map2 constructor is used directly.
 */
class Map2[K,L,V](val inner:Map[K,Map[L,V]]) extends Traversable[(K,L,V)] with ((K,L) => V) {
  
  /** Set of K-keys associated with at least one value L-key and value */
  def keySet = inner.keySet
  
  /** Sets a binding, overwriting any binding already present for (k,l) */
  def put(k:K, l:L, v:V): Map2[K,L,V] = {
    inner.get(k) match {
      case None =>
        new Map2[K,L,V](inner + (k -> Map(l -> v)))
      case Some(x) =>
        new Map2[K,L,V](inner + (k -> (x + (l -> v))))
    }
  }
  
  /** Changes the binding for (k,l).
   *  If no binding is present, f(None) is inserted.
   *  If (k,l) is bound to a value v, then f(Some(v)) is inserted. */
  def update(k:K, l:L, f:Option[V]=>V) = {
    inner.get(k) match {
      case None =>
        new Map2[K,L,V](inner + (k -> Map(l -> f(None))))
      case Some(x) =>
        new Map2[K,L,V](inner + (k -> (x + (l -> f(x.get(l))))))
    }
  }
  
  /** Returns the map of bindings associated with the given K-key.
   *  This is a total function and never returns null. */
  def get(k:K) : Map[L,V] = inner.get(k) match {
    case None => Map.empty
    case Some(x) => x
  }
  
  /** Returns the value associated with (k,l) or None if so such binding exists */
  def get(k:K, l:L) : Option[V] = get(k).get(l)
  
  /** Returns the value associated with (k,l) if it exists; otherwise the third
   *  argument is evaluated and returned. */
  def getOrElse(k:K, l:L, v : => V) : V = get(k, l) match {
    case None => v
    case Some(x) => x
  }
  
  def foreach[U](f : ((K,L,V)) => U) {
    inner foreach {case (k,m) =>
      m foreach {case (l,v) =>
        f ((k,l,v))
      }
    }
  }
  
  /** Filters out the K-keys and their images that don't satisfy the given predicate */
  def filter2(f : ((K, Map[L,V])) => Boolean): Map2[K,L,V] =
    new Map2[K,L,V](inner.filter(f))
  
  /** Returns the value associated with (k,l) or throws an exception if no such binding exists */
  def apply(k : K, l : L): V = inner(k)(l)
  
  /** Creates a map where the order of the two keys are reversed. Not a very fast operation. */
  def swapKeys: Map2[L,K,V] = {
    var m = new Map2[L,K,V](Map.empty)
    for ( (k,lv) <- inner) {
      for ( (l,v) <- lv) {
        m = m.put(l,k,v)
      }
    }
    m
  }

}

object Map2 {
  /** The empty map */
  def empty[K,L,V] = emptyImpl.asInstanceOf[Map2[K,L,V]]

  private val emptyImpl = new Map2[Any,Any,Any](Map.empty)
  
  /** A map with a single binding */
  def singleton[K,L,V](k:K,l:L,v:V) = new Map2[K,L,V](Map(k -> Map(l -> v)))
}