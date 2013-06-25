package dk.brics.lightrefactor.types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dk.brics.lightrefactor.util.MapExtensions;

public class ContextPropagation {
  
  private FunctionLca lca;
  private Set<TypeNode> allTypes = new HashSet<TypeNode>();
  
  public ContextPropagation(FunctionLca lca) {
    this.lca = lca;
  }
  
  public void propagate(Iterable<TypeNode> nodes) {
    Search search = new Search();
    // build predecessor map
    Set<TypeNode> visited = new HashSet<TypeNode>();
    for (TypeNode node : nodes) {
      node = node.rep();
      allTypes.add(node);
      MapExtensions.getSet(search.preds, node);
      if (visited.add(node)) {
        for (Map.Entry<String,TypeNode> en : node.prty.entrySet()) {
          MapExtensions.getSet(search.preds, en.getValue().rep()).add(node);
        }
      }
    }
    // propagate contexts
    for (TypeNode t : nodes) {
      search.search(t);
    }
  }
  
  private final class Search {
    Map<TypeNode, HashSet<TypeNode>> preds = new HashMap<TypeNode, HashSet<TypeNode>>();
    Set<TypeNode> visited = new HashSet<TypeNode>();
    
    void search(TypeNode t) {
      t = t.rep();
      if (!visited.add(t)) {
        return;
      }
      if (!allTypes.contains(t)) {
        throw new RuntimeException(t + "");
      }
      for (TypeNode pred : preds.get(t)) {
        search(pred);
        t.context = lca.lca(t.context, pred.context);
      }
    }
  }
  
  
}
