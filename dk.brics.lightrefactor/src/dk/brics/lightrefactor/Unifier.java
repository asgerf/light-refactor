package dk.brics.lightrefactor;

import java.util.LinkedList;
import java.util.Map;

public class Unifier {
  
  private LinkedList<UnifyNode> queue = new LinkedList<UnifyNode>();
  
  public void unify(UnifyNode x, UnifyNode y) {
    x = x.rep();
    y = y.rep();
    if (x == y)
      return;
    if (x.rank < y.rank) {
      UnifyNode tmp = x;
      x = y;
      y = tmp;
    } else if (x.rank == y.rank) {
      x.rank += 1;
    }
    y.parent = x;
    x.namespace |= y.namespace;
    Map<String,UnifyNode> src;
    Map<String,UnifyNode> dst;
    if (x.prty.size() < y.prty.size()) {
      dst = y.prty;
      src = x.prty;
      x.prty = dst;
    } else {
      dst = x.prty;
      src = y.prty;
    }
    y.prty = null;
    for (Map.Entry<String,UnifyNode> e : src.entrySet()) {
      UnifyNode existingNode = dst.get(e.getKey());
      if (existingNode == null) {
        dst.put(e.getKey(), e.getValue());
      } else {
        unifyLater(existingNode, e.getValue());
      }
    }
  }
  
  public void unifyPrty(UnifyNode x, String prty, UnifyNode y) {
    x = x.rep();
    UnifyNode xf = x.prty.get(prty);
    if (xf == null) {
      x.prty.put(prty, y);
    } else {
      unify(xf, y);
    }
  }
  
  public void unifyLater(UnifyNode x, UnifyNode y) {
    if (x != y) {
      queue.add(x);
      queue.add(y);
    }
  }
  
  public void complete() {
    while (!queue.isEmpty()) {
      UnifyNode x = queue.pop();
      UnifyNode y = queue.pop();
      unify(x,y);
    }
  }
  
}
