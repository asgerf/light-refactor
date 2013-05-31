package dk.brics.scalautil.mut

import scala.collection._
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.Set
import scala.collection.Map

class MultiMap[K,V] extends MultiMapRead[K,V] with Cloneable {
	private val map = new HashMap[K,HashSet[V]]
	
	/** Set of keys that have one or more bindings */
	def keySet = map.keySet
	
	/** Removes all bindings whose key is no in 'keys' */
	def retainKeys(keys: Set[K]) = map.retain((key,_) => keys.contains(key))
	
	/** Computes the keys of values that occur in one or more bindings. */
	def valueSet = {
	  val result = new HashSet[V]
	  map.foreach({case (k,vs) => result ++= vs})
	  result
	}
	
	/** True if no bindings are in the map */
	override def isEmpty = map.isEmpty
	
	/** Removes all bindings */
	def clear = map.clear
	
	/** Adds 'key' -> 'v' if it is not already there. Returns true if a new binding was added. */
	def add(key: K, value: V): Boolean = {
		map.get(key) match {
			case None =>
				val set = new mutable.HashSet[V];
				set.add(value);
				map.put(key,set);
				return true
			case Some(x) =>
				return x.add(value);
		}
	}
	
	/** 
	 * Adds 'key' -> 'v' for every 'v' in 'values'. 
	 * Returns true if at least one binding was added that was not already in the map. 
	 */
	def addAll(key: K, values: TraversableOnce[_ <: V]): Boolean = {
		adjust(key, x => {
			var changed = false
			values.foreach(v => {changed |= x.add(v)})
			changed
		})
	}
	
	/** Removes the 'key' -> 'value' binding if it exists. Returns true if it existed */
	def remove(key: K, value: V): Boolean = {
		map.get(key) match {
			case None =>
				return false
			case Some(x) =>
				if (x.remove(value)) {
					if (x.isEmpty) {
						map.remove(key);
					}
					return true;
				} else {
					return false;
				}
		}
	}
	
	/** Removes all bindings with 'key'. Returns true if any bindings were removed. */
	def removeKey(key: K): Boolean = {
		map.remove(key) match {
			case None => false
			case Some(_) => true
		}
	}
	
	/** True if the 'key' -> 'value' binding exists */ 
	def contains(key: K, value: V): Boolean = {
		map.get(key) match {
			case None => false
			case Some(x) => x.contains(value)
		}
	}
	
	/** True if 'key' has one or more bindings */
	def containsKey(key: K): Boolean = {
		map.contains(key)
	}
	
	/** True if there is a binding 'key' -> 'v' for every 'v' in 'values' */
	def containsAll(key: K, values: TraversableOnce[_ <: V]) = {
		values.forall(v => contains(key,v));
	}
	
	def get(key: K): Set[V] = {
		map.get(key) match {
			case None => Set.empty
			case Some(x) => x
		}
	}
	
	/**
	 * Inspects and modifies the image of 'key'. The HashSet reference 
	 * should not be kept after 'valuation' returns.
	 * @param key a key (with or without existing bindings)
	 * @param valuation invoked exactly once
	 * @return the value returned by valuation
	 */
	def adjust[A](key: K, f : HashSet[V] => A): A = {
		map.get(key) match {
			case None => 
				val x = new HashSet[V];
				val a = f(x);
				if (!x.isEmpty) {
					map.put(key,x);
				}
				a;
			case Some(x) =>
				val a = f(x)
				if (x.isEmpty) {
					map.remove(key);
				}
				a;
		}
	}
	
	/** Returns the underlying map instance. Updates will propagate to the returned instance. */
	def asMap: Map[K,Set[V]] = map
	
	/** Adds every binding in 'other' to this multimap. */
	def ++=(other: MultiMap[K,V]) {
	  other.map.foreach({case (k,vs) =>
	    map.get(k) match {
	      case None => map.put(k, vs.clone)
	      case Some(x) => x ++= vs
	    }
	  })
	}
  /** Adds every binding in 'other' to this multimap. Returns true if new bindings were added. */
  def addAll(other: MultiMapRead[K,V]): Boolean = {
    var changed = false
    other.asMap.foreach({case (k,vs) =>
      map.get(k) match {
        case None => {
          map.put(k, SetUtil.copy(vs))
          changed = true
        }
        case Some(x) => {changed |= SetUtil.addAll(x, vs)}
      }
    })
    return changed
  }
  
	
  /** Removes every binding in 'other' from this multimap. */
  def --=(other: MultiMap[K,V]) {
    other.map.foreach({case (k,vs) =>
      map.get(k) match {
        case None => {}
        case Some(x) => { 
          x --= vs
          if (x.isEmpty) {
            map.remove(k)
          }
        }
      }
    })
  }
	
  override def clone: MultiMap[K,V] = {
    val copy = new MultiMap[K,V]
    map.foreach({case (k,vs) => copy.map.put(k, vs.clone)})
    copy
  }
	
	// Function trait
	def apply(key: K) = get(key)
	
	// Traversable trait
	def foreach[U](f : ((K,V)) => U): Unit = {
		map.foreach({case (k,x) =>
			x.foreach(v => f(k,v))
		});
	}
	
	override def toString = map.toString
	
}

object MultiMap {
  def copy[K,V](m: MultiMapRead[K,V]): MultiMap[K,V] = {
    val c = new MultiMap[K,V]
    m.asMap.foreach({case (k,v) =>
      c.map.put(k, SetUtil.copy(v))
    })
    c
  }
}