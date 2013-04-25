package dk.brics.lightrefactor;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.NodeVisitor;

public class NodeFinder {
  public static AstNode find(final AstNode root, final int absolutePosition) {
    final AstNode[] result = new AstNode[] { root };
    
    root.visit(new NodeVisitor() {
      int pos = absolutePosition;
      @Override public boolean visit(AstNode node) {
//        if (node.getParent() != null && node.getParent() != result[0])
//          return false;
//        if (node.getParent() != null && node.getParent() != result[0]) {
//          throw new RuntimeException("Unexpected recursive call to " + node.getClass().getSimpleName());
//        }
        if (node.getPosition() <= pos && node.getPosition() + node.getLength() >= pos) {
          result[0] = node;
          pos -= node.getPosition();
          return true;
        } else {
          return false;
        }
      }
    });
    
    return result[0];
  }
}
