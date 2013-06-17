package dk.brics.lightrefactor.types;

import java.util.LinkedList;
import java.util.Map;


public class TypeUnifier {
  
  private LinkedList<TypeNode> typeQueue = new LinkedList<TypeNode>();
  
  public void unify(TypeNode x, TypeNode y) {
    x = x.rep();
    y = y.rep();
    if (x == y)
      return;
    if (x.rank < y.rank) {
      TypeNode tmp = x;
      x = y;
      y = tmp;
    } else if (x.rank == y.rank) {
      x.rank += 1;
    }
    y.parent = x;
    x.namespace |= y.namespace;
    if (x.functions == null) {
      x.functions = y.functions;
    } else if (y.functions != null) {
      x.functions.splice(y.functions);
    }
    Map<String,TypeNode> src;
    Map<String,TypeNode> dst;
    if (x.prty.size() < y.prty.size()) {
      dst = y.prty;
      src = x.prty;
      x.prty = dst;
    } else {
      dst = x.prty;
      src = y.prty;
    }
    y.prty = null;
    for (Map.Entry<String,TypeNode> e : src.entrySet()) {
      TypeNode existingNode = dst.get(e.getKey());
      if (existingNode == null) {
        dst.put(e.getKey(), e.getValue());
      } else {
        unifyLater(existingNode, e.getValue());
      }
    }
    if (x.supers.size() < y.supers.size()) {
      y.supers.addAll(x.supers);
      x.supers = y.supers;
    } else {
      x.supers.addAll(y.supers);
      y.supers = null;
    }
  }
  
  public void unifyPrty(TypeNode x, String prty, TypeNode y) {
    x = x.rep();
    TypeNode xf = x.prty.get(prty);
    if (xf == null) {
      x.prty.put(prty, y);
    } else {
      unify(xf, y);
    }
  }
  
  public void unifyLater(TypeNode x, TypeNode y) {
    if (x != y) {
      typeQueue.add(x);
      typeQueue.add(y);
    }
  }
  
  public void complete() {
    while (!typeQueue.isEmpty()) {
      TypeNode x = typeQueue.pop();
      TypeNode y = typeQueue.pop();
      unify(x,y);
    }
  }
  
}
