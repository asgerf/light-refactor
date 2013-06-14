package dk.brics.lightrefactor.util

import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.HashSet
import java.util.Iterator
import java.util.List
import java.util.Map
import java.util.Set

class ConcatIterator<T> implements Iterator<T> {
  var Iterator<T> current;
  var Iterable<T> continuation;
  
  new (Iterator<T> current, Iterable<T> continuation) {
    this.current = current;
    this.continuation = continuation;
  }
  
  override def hasNext() {
    if (current.hasNext) {
      return true
    } else if (continuation != null) {
      current = continuation.iterator()
      continuation = null
      return current.hasNext
    } else {
      return false
    }
  }
  
  override def next() {
    if (current.hasNext) {
      return current.next()
    } else if (continuation != null) {
      current = continuation.iterator()
      return current.next()
    } else {
      return current.next() // let the inner iterator throw an exception
    }
  }
  
  override def remove() {
    throw new UnsupportedOperationException()
  }
}

class ConcatIterable<T> implements Iterable<T> {
  val Iterable<T> left
  val Iterable<T> right
  
  new (Iterable<T> left, Iterable<T> right) {
    this.left = left
    this.right = right
  }
  
  override def iterator() {
    return new ConcatIterator<T>(left.iterator(), right);
  }
}

class MapExtensions {
  def static <K,L,V> HashMap<L,V> getMap(Map<K,HashMap<L,V>> map, K key) {
    var x = map.get(key)
    if (x == null) {
      x = new HashMap<L,V>
      map.put(key, x)
    }
    x
  }
  def static <K,L,V> Map<L,V> tryMap(Map<K,HashMap<L,V>> map, K key) {
    var x = map.get(key)
    if (x == null) {
      emptyMap
    } else {
      x
    }
  }
  
  def static <K,V> ArrayList<V> getList(Map<K,ArrayList<V>> map, K key) {
    var x = map.get(key)
    if (x == null) {
      x = new ArrayList<V>
      map.put(key, x)
    }
    x
  }
  def static <K,V> List<V> tryList(Map<K,ArrayList<V>> map, K key) {
    var x = map.get(key)
    if (x == null) {
      return emptyList
    } else {
      return x
    }
  }
  
  def static <K,V> HashSet<V> getSet(Map<K,HashSet<V>> map, K key) {
    var x = map.get(key)
    if (x == null) {
      x = new HashSet<V>
      map.put(key, x)
    }
    x
  }
  def static <K,V> Set<V> trySet(Map<K,HashSet<V>> map, K key) {
    var x = map.get(key)
    if (x == null) {
      return emptySet
    } else {
      return x
    }
  }
  
  def static<T> Iterable<T> concat(Iterable<T> xs, Iterable<T> ys) {
    return new ConcatIterable(xs,ys)
  }
  def static<T> Iterable<T> append(Iterable<T> xs, T y) {
    return new ConcatIterable(xs, Collections::singleton(y))
  }
  def static<T> Iterable<T> prepend(T x, Iterable<T> ys) {
    return new ConcatIterable(Collections::singleton(x),ys)
  }
}
