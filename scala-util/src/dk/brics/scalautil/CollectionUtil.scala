package dk.brics.scalautil

object CollectionUtil {
  /**
   * Merges two maps into one, while keeping track of changes with regard to the first argument.
   * 
   * For keys that are present in both maps, the given combine operation is used to decide 
   * on what value should be in the merged map. For keys that are present in only one map,
   * the binding is carried over directly.
   * 
   * The combine operation must return, in addition to the merged value, a boolean indicating
   * if the new value equals the first argument given to it.
   * 
   * This method returns, in addition to the merged map, a boolean indicating
   * if the new map differs from the first argument given to it.
   */
  def unionMaps[K,V](m1:Map[K,V], m2:Map[K,V], combine:(V,V) => (V,Boolean)): (Map[K,V], Boolean) = {
    val keys = m1.keySet ++ m2.keySet
    var result = Map.empty[K,V]
    var changed = false
    for (key <- keys) {
      (m1.get(key),m2.get(key)) match {
        case (None,None) => throw new RuntimeException()
        case (Some(v1),None) =>
          result += ((key,v1))
        case (None,Some(v2)) =>
          result += ((key,v2))
          changed = true
        case (Some(v1),Some(v2)) =>
          val (v,ch) = combine(v1,v2)
          changed |= ch
          result += ((key,v))
      }
    }
    (result,changed)
  }
}