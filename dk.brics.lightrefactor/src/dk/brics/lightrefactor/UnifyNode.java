package dk.brics.lightrefactor;

import java.util.HashMap;
import java.util.Map;

public final class UnifyNode {
  UnifyNode parent = this;
  int rank = 0;
  boolean namespace = false;
  Map<String,UnifyNode> prty = new HashMap<String,UnifyNode>();
  
  UnifyNode rep() {
    if (parent != this) {
      return parent = parent.rep();
    } else {
      return this;
    }
  }
}
