package dk.brics.scalautil.immut

import scala.collection.immutable._

/**
 * A relation between K and V, supporting fast lookup in the K to V direction.
 * It is closely related to the type Map[K,Set[V]], but with some minor differences.
 * 
 * In Map[K,Set[V]] a key can be either
 * - absent
 * - present and map to null
 * - present and map to the empty set
 * - present and map to a non-empty set
 * 
 * In MultiMap a key can be either
 * - absent because it maps to the empty set
 * - present because it maps to a non-empty set
 * 
 * Note that the invariant for MultiMap can be broken if the MultiMap constructor is used directly.
 */
class MultiMap[K,V](val inner: Map[K,Set[V]]) extends (K => Set[V]) with Traversable[(K,V)] {
	
  /** Set of keys with at least one associated value */
	def keySet = inner.keySet
	
	/** True if the multimap contains to bindings */
	override def isEmpty = inner.isEmpty
	
	/** Computes the of values present in at least one binding. Not very fast. */
	def valueSet : Set[V] = inner.foldLeft[Set[V]](Set.empty)({case (b,(k,vs)) => b ++ vs})
	
	/**
	 * Adds a binding to the multimap.
	 * @return the new multimap; and true if the binding was not already there (ie. the multimap changed)
	 */
	def add(key: K, value: V): (MultiMap[K,V],Boolean) = {
		inner.get(key) match {
			case None =>
				(new MultiMap(inner + ((key, Set(value)))), true)
			case Some(x) =>
				val x2 = x + value
				if (x2.size != x.size)
					(new MultiMap(inner + ((key, x2))), true)
				else
					(this, false)
		}
	}
	
	/**
	 * Adds all the specified values as bindings to the specified key.
	 * @return the new multimap
	 */
  def addAll(key: K, values : TraversableOnce[V]): (MultiMap[K,V], Boolean) = {
    var changed = false
    var result = this
    for ( v <- values) {
      val (result2,changed2) = result.add(key, v)
      result = result2
      changed |= changed2
    }
    (result,changed)
  }
	
	/**
	 * Left-biased union operation (i.e. performs best when left argument is biggest).
	 * @return the new multimap; and true if new multimap is distinct from the 'this' argument
	 */
	def addAll(other : MultiMap[K,V]): (MultiMap[K,V],Boolean) = {
		var m = inner
		var changed = false
		other.inner foreach {case (k,vs) => 
			inner.get(k) match {
				case None => 
					m += ((k, vs))
					changed = true
				case Some(x) =>
					val x2 = x ++ vs
					if (x2.size != x.size) {
						m += ((k, x2))
						changed = true
					}
			}
		}
		(new MultiMap[K,V](m), changed)
	}
	
	/**
	 * Removes a binding from the multimap if it is present.
	 * @return the new multimap; and true if a binding was removed (ie. the multimap changed)
	 */
	def remove(key: K, value: V): (MultiMap[K,V], Boolean) = {
		inner.get(key) match {
			case None => (this,false)
			case Some(x) =>
				val x2: Set[V] = x - value
				if (x2.size == 0)
					(new MultiMap[K,V](inner - key), true)
				else if (x2.size != x.size)
					(new MultiMap[K,V](inner + ((key, x2))), true)
				else
					(this,false)
		}
	}
	
	/**
	 * Removes all bindings for the specified key
	 * @return the new multimap; and true if any bindings were removed (ie. the multimap changed)
	 */
	def removeKey(key: K): (MultiMap[K,V], Boolean) = {
		val m2 = inner - key
		if (m2.size != inner.size)
			(new MultiMap[K,V](m2), true)
		else
			(this, false)
	}
	
	/**
	 * Removes all bindings present in the other multimap.
	 * @return the new multimap; and true if any bindings were removed (ie, the multimap changed)
	 */
	def removeAll(other: MultiMap[K,V]): (MultiMap[K,V],Boolean) = {
		var m = inner
		var changed = false
		other.inner foreach {case (k,vs) => 
			 inner.get(k) match {
				 case None => {}
				 case Some(x) =>
				 	val x2 = x -- vs
				 	if (x2.size == 0) {
				 		m -= k
				 		changed = true
				 	}
				 	else if (x2.size != x.size) {
				 		m += ((k, x2))
				 		changed = true
				 	}
			 }
		}
		if (changed)
			(new MultiMap[K,V](m), true)
		else
			(this, false)
	}
	
	/**
	 * Removes keys that don't match the specified predicate.
	 */
	def filterKeys(f : K => Boolean): MultiMap[K,V] = {
		new MultiMap[K,V](inner.filterKeys(f))
	}
	
	/** Adds a binding */
	def +(kv : (K,V)) = add(kv._1, kv._2)._1
	
	/** Union */
	def ++(m : MultiMap[K,V]) = addAll(m)._1
	
	/** Remove a binding */
	def -(kv: (K,V)) = remove(kv._1, kv._2)._1
	
	/** Remove a key */
	def -(k: K) = removeKey(k)._1
	
	/** Subtract another relation */
	def --(m: MultiMap[K,V]) = removeAll(m)._1
	
	/** Returns the set of values associated with the given key.
	 *  This is a total function and never returns null. */
	def get(key: K): Set[V] = {
		inner.get(key) match {
			case None => Set.empty
			case Some(x) => x
		}
	}
	
	/** True if the given value is associated with the given key */
	def contains(key: K, value: V) = {
		inner.get(key) match {
			case None => false
			case Some(x) => x.contains(value)
		}
	}
	
	/** True if at least one value is associated with the given key */
	def containsKey(key: K) = inner.contains(key)
	
	/** True if this multimap contains all bindings present in the other multimap */
	def containsAll(other: MultiMap[K,V]): Boolean = {
		other.inner foreach {case (k,vs) =>
			inner.get(k) match {
				case None => {
					return false
				}
				case Some(x) =>
					if (!vs.subsetOf(x))
						return false
			}
		}
		true
	}
	
	/** True if the other multimap contains all bindings present in this multimap */
	def submapOf(other: MultiMap[K,V]) = other.containsAll(this)
	
	/** Synonym of get. Returns the set of values associated with the given key. */
	def apply(key: K) = get(key)
	
	/** Iterates all key/value pairs */
	def foreach[U](f : ((K,V)) => U) {
		inner.foreach({case (k,vs) => 
			vs.foreach(v => f(k,v))
		})
	}

  /** Computes the inverse multimap. Takes roughly linear time in the size of the multimap. */
  def inverse : MultiMap[V,K] = {
    var result = MultiMap.empty[V,K]
    foreach {case (k,v) =>
      result += ((v,k))
    }
    result
  }

  override def equals(that : Any) = that match {
    case other:MultiMap[_,_] => other.inner equals inner
    case _ => false
  }
  override def hashCode = inner.hashCode
}

object MultiMap {
  /** A multimap with one binding */
	def singleton[K,V](key: K, value:V) = new MultiMap[K,V](Map((key,Set(value))))
	
	/** Synonym for singleton. Creates a multimap with one binding. */
	def apply[K,V](key:K, value:V) = singleton(key,value)
	
	/** The empty multimap */
	def empty[K,V] = new MultiMap[K,V](Map.empty)
	
	def make[K,V](kvs:(K,V)*) = fromList(kvs)
	
	/** Creates a multimap with the given bindings */
	def fromList[K,V](kvs:TraversableOnce[(K,V)]) = {
	  var m = MultiMap.empty[K,V]
	  for ( (k,v) <- kvs) {
	    m += ((k,v))
	  }
	  m
	}
	
}
