package dk.brics.scalautil.immut

import scala.collection._

/**
 * A ternary relation between K, L, and V (ie. a set of triples), supporting fast lookup in the K to L to V direction.
 * 
 * See MultiMap for the differences between MultiMaps and Maps with Sets in them.
 */
class MultiMap2[K,L,V](val map: immutable.Map[K,MultiMap[L,V]]) extends (K => MultiMap[L,V]) with Traversable[(K,L,V)] {
	
	def keySet = map.keySet
	
	override def isEmpty = map.isEmpty

  /** Set of values V for which at least one (k1,k2) pair maps to V */
  def valueSet = map.foldLeft[Set[V]](Set.empty)({case (b,(k,m)) => b ++ m.valueSet})
	
	def add(key: K, ley: L, value: V): (MultiMap2[K,L,V],Boolean) = {
		map.get(key) match {
			case None =>
				(new MultiMap2(map + ((key, MultiMap(ley, value)))), true)
			case Some(x) =>
				val (x2,changed) = x.add(ley,value)
				if (changed)
					(new MultiMap2(map + ((key, x2))), true)
				else
					(this, false)
		}
	}
	
	/**
	 * Left-biased union operation (i.e. performs best when left argument is biggest).
	 * @return new multi-map, and true if new multi-map is distinct from the 'this' argument
	 */
	def addAll(other : MultiMap2[K,L,V]): (MultiMap2[K,L,V],Boolean) = {
		var m = map
		var changed = false
		other.map foreach {case (k,vs) => 
			map.get(k) match {
				case None => 
					m += ((k, vs))
					changed = true
				case Some(x) =>
					val (x2,ch) = x.addAll(vs)
					if (ch) {
						m += ((k, x2))
						changed = true
					}
			}
		}
		(new MultiMap2[K,L,V](m), changed)
	}
	
	def remove(key: K, ley: L, value: V): (MultiMap2[K,L,V], Boolean) = {
		map.get(key) match {
			case None => (this,false)
			case Some(x) =>
				val (x2,changed) = x.remove(ley, value)
				if (x2.isEmpty)
					(new MultiMap2[K,L,V](map - key), true)
				else if (changed)
					(new MultiMap2[K,L,V](map + ((key, x2))), true)
				else
					(this,false)
		}
	}
	
	def removeKey(key: K): (MultiMap2[K,L,V], Boolean) = {
		val m2 = map - key
		if (m2.size != map.size)
			(new MultiMap2[K,L,V](m2), true)
		else
			(this, false)
	}
	
	def removeKey(key: K, ley: L): (MultiMap2[K,L,V], Boolean) = {
		map.get(key) match {
			case None => (this,false)
			case Some(x) =>
				val (x2,changed) = x.removeKey(ley)
				if (x2.isEmpty)
					(new MultiMap2[K,L,V](map - key), true)
				else if (changed)
					(new MultiMap2[K,L,V](map + ((key,x2))), true)
				else
					(this, false)
		}
	}
	
	def removeAll(other: MultiMap2[K,L,V]): (MultiMap2[K,L,V],Boolean) = {
		var m = map
		var changed = false
		other.map foreach {case (k,vs) => 
			 map.get(k) match {
				 case None => {}
				 case Some(x) =>
				 	val (x2,ch) = x.removeAll(vs)
				 	if (x2.isEmpty) {
				 		m -= k
				 		changed = true
				 	}
				 	else if (ch) {
				 		m += ((k, x2))
				 		changed = true
				 	}
			 }
		}
		if (changed)
			(new MultiMap2[K,L,V](m), true)
		else
			(this, false)
	}
	
	def filterKeys(f: K => Boolean): MultiMap2[K,L,V] = {
		new MultiMap2[K,L,V](map.filterKeys(f))
	}
	
	def +(kv : (K,L,V)) = add(kv._1, kv._2, kv._3)._1
	def ++(m : MultiMap2[K,L,V]) = addAll(m)._1
	def -(kv: (K,L,V)) = remove(kv._1, kv._2, kv._3)._1
	def -(k: K) = removeKey(k)._1
	def --(m: MultiMap2[K,L,V]) = removeAll(m)._1
	
	def get(key: K): MultiMap[L,V] = {
		map.get(key) match {
			case None => MultiMap.empty
			case Some(x) => x
		}
	}
	def get(key: K, ley: L): Set[V] = {
		map.get(key) match {
			case None => Set.empty
			case Some(x) => x.get(ley)
		}
	}
	
	
	def contains(key: K, ley:L, value: V) = {
		map.get(key) match {
			case None => false
			case Some(x) => x.contains(ley, value)
		}
	}
	
	def containsKey(key: K) = map.contains(key)
	
	def containsKey(key: K, ley: L) = {
		map.get(key) match {
			case None => false
			case Some(x) => x.containsKey(ley)
		}
	}
	
	def containsAll(other: MultiMap2[K,L,V]): Boolean = {
		other.map foreach {case (k,vs) =>
			map.get(k) match {
				case None => {
					return false
				}
				case Some(x) =>
					if (!x.containsAll(vs))
						return false
			}
		}
		true
	}
	
	def submapOf(other: MultiMap2[K,L,V]) = other.containsAll(this)
	
	def apply(key: K) = get(key)
	
	def foreach[U](f : ((K,L,V)) => U) {
		map foreach {case (k,vs) => 
			vs foreach {case (l,v) =>
				f(k,l,v)
			}
		}
	}

  def inverse : MultiMap2[V, L, K] = {
    var m = MultiMap2.empty[V,L,K]
    foreach {case (k1,k2,v) =>
      m += ((v,k2,k1))
    }
    m
  }

  def filtered(f : ((K,L,V)) => Boolean) = {
    var result = this
    for ( (k,lv) <- map) {
      for ( (l,v) <- lv) {
        if (!f((k,l,v))) {
          result -= ((k,l,v))
        }
      }
    }
    result
  }

  override def equals(that : Any) = that match {
    case other:MultiMap2[_,_,_] => other.map equals map
    case _ => false
  }
  override def hashCode = map.hashCode
}

object MultiMap2 {
	def empty[K,L,V] = new MultiMap2[K,L,V](immutable.Map.empty)

  def singleton[K,L,V](k:K,l:L,v:V) = new MultiMap2[K,L,V](immutable.Map(k -> MultiMap.singleton(l, v)))

  def fromList[K,L,V](list:TraversableOnce[(K,L,V)]): MultiMap2[K, L, V] = {
    var m = MultiMap2.empty[K,L,V]
    for ( x <- list) {
      m += x
    }
    m
  }

  def apply[K,L,V](list : (K,L,V)*): MultiMap2[K, L, V] = {
    fromList(list)
  }
}