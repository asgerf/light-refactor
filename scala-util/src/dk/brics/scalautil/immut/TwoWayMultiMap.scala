package dk.brics.scalautil.immut

/**
 * A multimap that supports fast lookup in both directions.
 * 
 * Semantically it is equivalent to MultiMap, but has different time/space usage.
 * 
 * @see MultiMap
 */
class TwoWayMultiMap[K,V](val forw:MultiMap[K,V], val backw:MultiMap[V,K]) {
  
  /** Set of keys with at least one binding. */
  def keySet = forw.keySet
  
  /** Set of values with at least one binding. */
  def valueSet = backw.keySet
  
  /** True if the multimap is empty */
  def isEmpty = forw.isEmpty
  
  /**
   * Adds a binding to the multimap.
   * @return the new multimap; and true if the binding was not already there (ie. the multimap changed)
   */
  def add(key:K, value:V) : (TwoWayMultiMap[K,V],Boolean) = {
    val (f,c) = forw.add(key,value)
    if (c) {
      val (b,_) = backw.add(value,key)
      (new TwoWayMultiMap[K,V](f,b), true)
    } else {
      (this, false)
    }
  }
  
  /**
   * Adds all the specified values as bindings to the specified key.
   * @return the new multimap
   */
  def addAll(key: K, values : TraversableOnce[V]): TwoWayMultiMap[K,V] = {
    var result = this
    for ( v <- values) {
      result += ((key,v))
    }
    result
  }
  
  /**
   * Left-biased union operation (i.e. performs best when left argument is biggest).
   * @return the new multimap; and true if new multimap is distinct from the 'this' argument
   */
  def addAll(other : TwoWayMultiMap[K,V]): (TwoWayMultiMap[K,V],Boolean) = {
    // TODO: Can be made more efficient by merging forw.addAll and backw.addAll
    val (f,c) = forw.addAll(other.forw)
    if (c) {
      val (b,_) = backw.addAll(other.backw)
      (new TwoWayMultiMap[K,V](f,b), true)
    } else {
      (this, false)
    }
  }
  
  /**
   * Removes a binding from the multimap if it is present.
   * @return the new multimap; and true if a binding was removed (ie. the multimap changed)
   */
  def remove(key:K, value:V): (TwoWayMultiMap[K,V], Boolean) = {
    val (f,c) = forw.remove(key,value)
    if (c) {
      val (b,_) = backw.remove(value,key)
      (new TwoWayMultiMap[K,V](f,b), true)
    } else {
      (this, false)
    }
  }
  
  /**
   * Removes all bindings for the specified key
   * @return the new multimap; and true if any bindings were removed (ie. the multimap changed)
   */
  def removeKey(key:K): (TwoWayMultiMap[K,V], Boolean) = {
    forw.inner.get(key) match {
      case None => (this,false)
      case Some(values) =>
        val (f,_) = forw.removeKey(key)
        var b = backw
        for ( v <- values) {
          b -= v
        }
        (new TwoWayMultiMap[K,V](f,b), true)
    }
  }
  
  /**
   * Removes all bindings for the specified value
   * @return the new multimap; and true if any bindings were removed (ie. the multimap changed)
   */
  def removeValue(value:V) : (TwoWayMultiMap[K,V], Boolean) = {
    backw.inner.get(value) match {
      case None => (this,false)
      case Some(keys) =>
        val (b,_) = backw.removeKey(value)
        var f = forw
        for ( k <- keys) {
          f -= k
        }
        (new TwoWayMultiMap[K,V](f,b), true)
    }
  }
  
  /** Adds a binding */
  def +(kv : (K,V)) = add(kv._1, kv._2)._1
  
  /** Union */
  def ++(m : TwoWayMultiMap[K,V]) = addAll(m)._1
  
  /** Removes a binding */
  def -(kv: (K,V)) = remove(kv._1, kv._2)._1
  
  /** Removes a key */
  def -(k: K) = removeKey(k)._1
  
  /** Returns the set of values associated with the given key.
   *  This is a total function and never returns null. 
   *  It can be more convenient to use "m.forw(key)" instead of "m.get(key)". */
  def get(key: K): Set[V] = forw.get(key)
  
  /** Returns the set of keys associated with the given value.
   *  This is a total function and never returns null. 
   *  It can be more convenient to use "m.backw(key)" instead of "m.getInverse(key)". */
  def getInverse(value: V): Set[K] = backw.get(value)
  
  /** True if the given value is associated with the given key */
  def contains(key: K, value: V) = forw.contains(key,value)
  
  /** True if at least one value is associated with the given key */
  def containsKey(key: K) = forw.containsKey(key)
  
  /** True if at least one key is associated with the given value */
  def containsValue(value: V) = backw.containsKey(value)
  
  /** Returns the inverse map. This is a constant-time operation. */
  def inverse: TwoWayMultiMap[V,K] = new TwoWayMultiMap[V,K](backw,forw)
  
  override def equals(that : Any) = that match {
    case other:TwoWayMultiMap[_,_] => other.forw equals forw
    case _ => false
  }
  override def hashCode = forw.hashCode
}

object TwoWayMultiMap {
  private val emptyVal = new TwoWayMultiMap[Any,Any](MultiMap.empty, MultiMap.empty)
  
  /** The empty multimap */
  def empty[K,V] = emptyVal.asInstanceOf[TwoWayMultiMap[K,V]]
  
  /** A multimap with one binding */
  def singleton[K,V](key: K, value:V) = new TwoWayMultiMap[K,V](
      MultiMap.singleton(key,value),
      MultiMap.singleton(value,key)
      )
      
  /** Synonym for singleton. Creates a multimap with one binding. */
  def apply[K,V](key:K, value:V) = singleton(key,value)
  
  /** Creates a multimap with the given bindings */
  def fromList[K,V](kvs:(K,V)*): TwoWayMultiMap[K,V] = {
    var forw = MultiMap.empty[K,V]
    var backw = MultiMap.empty[V,K]
    for ( (k,v) <- kvs) {
      forw += ((k,v))
      backw += ((v,k))
    }
    new TwoWayMultiMap[K,V](forw,backw)
  }
}
