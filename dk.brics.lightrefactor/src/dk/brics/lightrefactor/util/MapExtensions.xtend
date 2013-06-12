package dk.brics.lightrefactor.util

import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet
import java.util.List
import java.util.Map
import java.util.Set

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
}
