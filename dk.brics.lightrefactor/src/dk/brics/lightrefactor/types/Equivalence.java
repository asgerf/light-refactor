package dk.brics.lightrefactor.types;

import java.util.HashMap;
import java.util.Map;

final class EquivalenceNode<T> {
  EquivalenceNode<T> parent = this;
  int rank = 0;
  T item;
  
  public EquivalenceNode(T item) {
    this.item = item;
  }
  
  public EquivalenceNode<T> rep() {
    if (parent == this) {
      return this;
    } else{
      return this.parent = parent.rep();
    }
  }
}

public class Equivalence<T> {
  
  private Map<T, EquivalenceNode<T>> map = new HashMap<T, EquivalenceNode<T>>();
  
  private EquivalenceNode<T> getNode(T x) {
    EquivalenceNode<T> node = map.get(x);
    if (node == null) {
      node = new EquivalenceNode<T>(x);
      map.put(x, node);
    }
    return node;
  }
  
  public boolean unify(T x, T y) {
    EquivalenceNode<T> xn = getNode(x).rep();
    EquivalenceNode<T> yn = getNode(y).rep();
    if (xn == yn)
      return false;
    if (xn.rank < yn.rank) {
      EquivalenceNode<T> z = xn;
      xn = yn;
      yn = z;
    } else if (xn.rank == yn.rank) {
      xn.rank += 1;
    }
    yn.parent = xn;
    return true;
  }
  
  public T getRep(T x) {
    return getNode(x).rep().item;
  }
  
  public Map<T,T> getResult() {
    Map<T,T> result = new HashMap<T,T>();
    for (Map.Entry<T, EquivalenceNode<T>> en : map.entrySet()) {
      result.put(en.getKey(), en.getValue().rep().item);
    }
    return result;
  }
}
