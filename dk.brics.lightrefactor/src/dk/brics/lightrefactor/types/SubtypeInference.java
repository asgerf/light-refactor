package dk.brics.lightrefactor.types;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

public class SubtypeInference {
  
  private TypeUnifier unifier;
  
  public SubtypeInference(TypeUnifier unifier) {
    this.unifier = unifier;
  }
  
  private void unify(TypeNode x, TypeNode y) {
    unifier.unify(x,y);
  }
  
  public boolean inferSubTypes(Collection<TypeNode> typeNodes) {
    Set<TypeNode> debug = new HashSet<TypeNode>();
    boolean performedChanges = false;
    boolean changed = true;
    while (changed) {
      changed = false;
      SCC scc = new SCC();
      for (TypeNode tn : typeNodes) {
        tn = tn.rep();
        scc.visit(tn);
      }
      changed |= unifier.complete();
      int numNewEdges = 0;
      for (SubTypeItem sti : scc.newSubTypes) {
        TypeNode sub = sti.sub.rep();
//        changed |= sub.supers.addAll(sti.supers);
        for (TypeNode sup : sti.supers) {
          sup = sup.rep();
          if (sup != sub) {
            if (sub.supers.add(sup)) {
              debug.add(sup);
              debug.add(sub);
              changed = true;
              numNewEdges++;
            }
//            changed |= sub.supers.add(sup);
          }
        }
      }
      performedChanges |= changed;
    }
    return performedChanges;
  }
  
  private static class SubTypeItem {
    TypeNode sub;
    Collection<TypeNode> supers;
    SubTypeItem(TypeNode sub, Collection<TypeNode> supers) {
      this.sub = sub;
      this.supers = supers;
    }
  }
  
  private final class SCC {
    private int numCycles = 0;
    private int index = 0;
    private final HashMap<TypeNode, Integer> node2index = new HashMap<TypeNode, Integer>();
    private final HashMap<TypeNode, Integer> node2lowlink = new HashMap<TypeNode, Integer>();
    private final Stack<TypeNode> stack = new Stack<TypeNode>();
    private final HashSet<TypeNode> onstack = new HashSet<TypeNode>();
    
    private final ArrayList<SubTypeItem> newSubTypes = new ArrayList<SubTypeItem>();
    private final HashMap<TypeNode, Map<String, Set<TypeNode>>> node2inherited = new HashMap<TypeNode, Map<String, Set<TypeNode>>>();
    
    private void addSupers(TypeNode x, Collection<TypeNode> supers) {
      newSubTypes.add(new SubTypeItem(x, supers));
    }
    
    public void visit(TypeNode v) {
      if (!node2index.containsKey(v)) {
        search(v);
      }
    }
    
    private void search(TypeNode v) {
      if (v.parent != v) 
        throw new RuntimeException("Not repr");
      node2index.put(v, index);
      node2lowlink.put(v, index);
      index++;
      stack.push(v);
      onstack.add(v);
      
      Map<String, Set<TypeNode>> inherited = new HashMap<String, Set<TypeNode>>();
      node2inherited.put(v, inherited);
      
      for (Map.Entry<String,TypeNode> en : v.prty.entrySet()) {
        inherited.put(en.getKey(), Collections.singleton(en.getValue().rep()));
      }
      
      Set<String> copied = new HashSet<String>(); // inherit sets for which we have a private copy (as opposed to a shared one)
      
      List<TypeNode> updatedNodes = new ArrayList<TypeNode>();
      for (Iterator<TypeNode> it = v.supers.iterator(); it.hasNext();) {
        TypeNode w = it.next();
        boolean isStale = w.parent != w;
        boolean isCollapsing = false;
        w = w.rep();
        if (v == w) {
          it.remove(); // subtype of self
          continue;
        }
        if (!node2index.containsKey(w)) {
          search(w);
          node2lowlink.put(v, Math.min(node2lowlink.get(v), node2lowlink.get(w)));
          isCollapsing = node2lowlink.get(v) == node2lowlink.get(w);
        } else if (onstack.contains(w)) {
          node2lowlink.put(v, Math.min(node2lowlink.get(v), node2index.get(w)));
          isCollapsing = true;
        }
        if (isStale) {
          it.remove();
          updatedNodes.add(w);
        }
//        if (isStale || isCollapsing) {
//          it.remove();
//          if (!isCollapsing && v != w) {
//            updatedNodes.add(w);
//          }
//        }
        if (!isCollapsing) {
          for (Map.Entry<String,Set<TypeNode>> en : node2inherited.get(w).entrySet()) {
            TypeNode own = v.prty.get(en.getKey());
            if (own != null) {
              addSupers(own, en.getValue());
            } else {
              Set<TypeNode> inheritSet = inherited.get(en.getKey());
              if (inheritSet != null && inheritSet != en.getValue()) {
                Iterator<TypeNode> tt = en.getValue().iterator();
                
                // skip all that are aleady in the set
                while (tt.hasNext() && inheritSet.contains(tt.next())) {}
                
                // if non-member found, create private copy, unless a copy was already made
                if (tt.hasNext() && copied.add(en.getKey())) {
                  inheritSet = new HashSet<TypeNode>(inheritSet);
                  inherited.put(en.getKey(), inheritSet);
                }
                
                // add remaining members
                while (tt.hasNext()) {
                  inheritSet.add(tt.next());
                }
              } else {
                inherited.put(en.getKey(), en.getValue());
              }
            }
          }
        }
      }
      v.supers.addAll(updatedNodes);
      
      if (node2lowlink.get(v) == node2index.get(v)) {
        // I'm not in a cycle or I am its root
        TypeNode w = stack.pop();
        onstack.remove(w);
        if (w != v) numCycles++;
        while (w != v) {
          unify(v,w);
          w = stack.pop();
          onstack.remove(w);
        }
      } else {
        // I'm part of a cycle and I'm not the root
      }
    }
    
  }
  
  
  
  
  
  
}
