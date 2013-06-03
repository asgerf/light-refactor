package dk.brics.scalautil.mut

import scala.collection._

/**
 * A read-only interface to a mutable multimap.
 * <p/>
 * An object of this type can often be downcast to MultiMap, but you should avoid doing this.
 */
trait MultiMapRead[K,V] extends (K => Set[V]) with Traversable[(K,V)] {
	def contains(key: K, value: V): Boolean
	def containsKey(key: K): Boolean
	def containsAll(key: K, values: TraversableOnce[_ <: V]): Boolean
	
	def get(key: K): Set[V]
	
	def isEmpty: Boolean
	
	def keySet: Set[K]
	def valueSet: Set[V]
	
	def asMap: Map[K,Set[V]]
}

object MultiMapRead {
	def empty[K,V] = EmptyMultiMap.asInstanceOf[MultiMapRead[K,V]]
	
	private object EmptyMultiMap extends MultiMapRead[Any,Any] {
		def contains(key:Any,value:Any) = false
		def containsKey(key:Any) = false
		def containsAll(key:Any, values:TraversableOnce[_]) = false
		def get(key:Any) = Set.empty
		override def isEmpty = true
		def keySet = Set.empty
		def valueSet = Set.empty
		def asMap = Map.empty
		def apply(k:Any) = Set.empty
		def foreach[U](f:((Any,Any)) => U) {}
	}
}
