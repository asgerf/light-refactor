package dk.brics.lightrefactor.types;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class SubtypeInference {
  
  private TypeUnifier unifier = new TypeUnifier();
  
  
  private void unify(TypeNode x, TypeNode y) {
    
  }
  
  private final class SCC {
    private int index = 0;
    private final HashMap<TypeNode, Integer> node2index = new HashMap<TypeNode, Integer>();
    private final HashMap<TypeNode, Integer> node2lowlink = new HashMap<TypeNode, Integer>();
    private final Stack<TypeNode> stack = new Stack<TypeNode>();
    private final HashSet<TypeNode> onstack = new HashSet<TypeNode>();
    
    private final ArrayList<TypeNode> newSubTypes = new ArrayList<TypeNode>(); // pair list
    
    private void registerSubType(TypeNode x, TypeNode y) {
      newSubTypes.add(x);
      newSubTypes.add(y);
    }
    
    private void search(TypeNode v) {
      node2index.put(v, index);
      node2lowlink.put(v, index);
      index++;
      stack.push(v);
      onstack.add(v);
      
      List<TypeNode> updatedNodes = new ArrayList<TypeNode>();
      for (Iterator<TypeNode> it = v.supers.iterator(); it.hasNext();) {
        TypeNode w = it.next();
        boolean isStale = w.parent != w;
        boolean isCollapsing = false;
        w = w.rep();
        // TODO: filter stale superTypes and/or ask for rep()
        if (!node2index.containsKey(w)) {
          search(w);
          node2lowlink.put(v, Math.min(node2lowlink.get(v), node2lowlink.get(w)));
          isCollapsing = node2lowlink.get(v) == node2lowlink.get(w);
        } else if (onstack.contains(w)) {
          node2lowlink.put(v, Math.min(node2lowlink.get(v), node2index.get(w)));
          isCollapsing = true;
        }
        if (isStale || isCollapsing) {
          it.remove();
          if (!isCollapsing) {
            updatedNodes.add(w);
          }
        }
        if (!isCollapsing) {
          
        }
      }
      v.supers.addAll(updatedNodes);
      
      if (node2lowlink.get(v) == node2index.get(v)) {
        // I'm not in a cycle or I am its root
        TypeNode w = stack.pop();
        while (w != v) {
          unify(v,w);
          w = stack.pop();
        }
      } else {
        // I'm part of a cycle and I'm not the root
      }
    }
    
  }
  
  
  
  
  
  
}
