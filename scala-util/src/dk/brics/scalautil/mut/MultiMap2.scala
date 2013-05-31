package dk.brics.scalautil.mut


import scala.collection._

class MultiMap2[K,L,V] extends ((K,L) => Set[V]) with Traversable[(K,L,V)] {
	private val map = new mutable.HashMap[K,MultiMap[L,V]];
	
	def get(key: K): MultiMapRead[L,V] = {
		map.get(key) match {
			case None => MultiMapRead.empty
			case Some(x) => x
		}
	}
	def get(key: K, ley: L): Set[V] = {
		map.get(key) match {
			case None => Set.empty
			case Some(x) => x.get(ley)
		}
	}
	
	def add(key: K, ley: L, value: V): Boolean = {
		map.get(key) match {
			case None =>
				val x = new MultiMap[L,V]
				x.add(ley,value)
				map.put(key,x)
				true
			case Some(x) =>
				x.add(ley,value)
		}
	}
	
	def addAll(key: K, values: MultiMapRead[L,V]): Boolean = {
	  map.get(key) match {
	    case None =>
	      if (!values.isEmpty) {
  	      map.put(key, MultiMap.copy(values))
  	      true
	      } else {
	        false
	      }
	    case Some(x) =>
	      x.addAll(values)
	  }
	}
	
	def remove(key: K, ley: L, value: V): Boolean = {
		map.get(key) match {
			case None =>
				false
			case Some(x) =>
				if (x.remove(ley,value)) {
					if (x.isEmpty) {
						map.remove(key)
					}
					true
				} else {
					false
				}
		}
	}
	
	def clear = map.clear
	def keySet = map.keySet
	override def isEmpty = map.isEmpty
	
	
	def contains(key: K, ley: L, value: V): Boolean = {
		map.get(key) match {
			case None => false
			case Some(x) => x.contains(ley,value)
		}
	}
	
	def containsKey(key: K): Boolean = {
		map.contains(key)
	}
	def containsKey(key: K, ley: L): Boolean = {
		map.get(key) match {
			case None => false
			case Some(x) => x.containsKey(ley)
		}
	}
	
  /** Adds every binding in 'other' to this multimap. */
  def ++=(other: MultiMap2[K,L,V]) {
    other.map.foreach({case (k,vs) =>
      map.get(k) match {
        case None => map.put(k, vs.clone)
        case Some(x) => x ++= vs
      }
    })
  }
  /** Adds every binding in 'other' to this multimap. Returns true if new bindings were added. */
  def addAll(other: MultiMap2[K,L,V]): Boolean = {
    var changed = false
    other.map.foreach({case (k,vs) =>
      map.get(k) match {
        case None => {
          map.put(k, vs.clone)
          changed = true
        }
        case Some(x) => {changed |= x.addAll(vs)}
      }
    })
    return changed
  }
  
  /** Removes every binding in 'other' from this multimap. */
  def --=(other: MultiMap2[K,L,V]) {
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
  
  /**
   * Computes the inverse of this map. 
   * Takes roughly linear time in the number of bindings.
   */
  def inverse: MultiMap2[V,L,K] = {
    val result = new MultiMap2[V,L,K]
    foreach({case (x,y,z) => result.add(z,y,x)})
    result
  }
  
  override def clone: MultiMap2[K,L,V] = {
    val copy = new MultiMap2[K,L,V]
    map.foreach({case (k,vs) => copy.map.put(k, vs.clone)})
    copy
  }
	
	// Function trait
	def apply(key: K, ley: L): Set[V] = get(key,ley)
	
	// Traversable trait
	def foreach[U](f : ((K,L,V)) => U) {
		map.foreach({case (k,m) => 
			m.foreach({case (l,v) => f(k,l,v)})
		})
	}
}