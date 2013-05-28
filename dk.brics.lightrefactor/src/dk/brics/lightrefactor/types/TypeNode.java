package dk.brics.lightrefactor.types;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public final class TypeNode {
  TypeNode parent = this;
  int rank = 0;
  boolean namespace = false;
  Map<String,TypeNode> prty = new HashMap<String,TypeNode>();
  
  TypeNode rep() {
    if (parent != this) {
      return parent = parent.rep();
    } else {
      return this;
    }
  }
}
