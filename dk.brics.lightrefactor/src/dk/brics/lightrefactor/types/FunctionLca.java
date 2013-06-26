package dk.brics.lightrefactor.types;

import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.FunctionNode;
import org.mozilla.javascript.ast.NodeVisitor;

import dk.brics.lightrefactor.Asts;

/**
 * Least common ancestor data structure for function nesting
 */
public class FunctionLca {
  
  private Map<FunctionNode, Integer> fun2depth = new HashMap<FunctionNode, Integer>();
  
  class Visitor implements NodeVisitor {
    int depth;
    public Visitor(int depth) {
      this.depth = depth;
    }
    public boolean visit(AstNode node) {
      if (node instanceof FunctionNode) {
        FunctionNode fun = (FunctionNode)node;
        fun2depth.put(fun, depth);
        fun.getBody().visit(new Visitor(depth+1));
        return false;
      } else {
        return true;
      }
    }
  }
  
  public FunctionLca(Asts asts) {
    asts.visit(new Visitor(0));
  }
  
  /**
   * Least common ancestor of a and b or null if top-level.
   */
  public FunctionNode lca(FunctionNode a, FunctionNode b) {
    if (a == null || b == null)
      return null;
    int ad = fun2depth.get(a);
    int bd = fun2depth.get(b);
    while (ad < bd) {
      b = b.getEnclosingFunction();
      bd--;
    }
    while (bd < ad) {
      a = a.getEnclosingFunction();
      ad--;
    }
    while (a != b) { // if no common ancestor, both will become null simultaneously
      a = a.getEnclosingFunction();
      b = b.getEnclosingFunction();
    }
    return a;
  }
  
  public Context lca(Context a, Context b) {
    if (a == Context.TopLevel || b == Context.TopLevel)
      return Context.TopLevel;
    else if (b == Context.None)
      return a;
    else if (a == Context.None)
      return b;
    else
      return Context.function(lca(a.getFunction(), b.getFunction()));
  }
  
  public boolean isAncestorOf(FunctionNode anc, FunctionNode child) {
    return lca(anc,child) == anc;
  }
  public boolean isAncestorOf(Context anc, Context child) {
    return lca(anc,child).equals(anc);
  }
  
}
