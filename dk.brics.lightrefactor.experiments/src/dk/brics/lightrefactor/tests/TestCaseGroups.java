package dk.brics.lightrefactor.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.AstRoot;

import dk.brics.lightrefactor.NameRef;
import dk.brics.lightrefactor.VisitAll;

public class TestCaseGroups {
  enum ScanState {
    Init, Slash, InsideComment, InsideCommentStar
  }
  private static String scanComment(String src, int index) {
    int i = index;
    ScanState state = ScanState.Init;
    int start = -1, end = -1;
    char commentType = '\0';
    while (i < src.length()) {
      char c = src.charAt(i);
      switch (state) {
      case Init:
        switch (c) {
        case ' ':
        case '\t':
        case '(':
        case ')':
        case ';':
          break;
        case '/':
          state = ScanState.Slash;
          break;
        default:
          return null;
        }
        break;
        
      case Slash:
        switch (c) {
        case '/':
        case '*':
          state = ScanState.InsideComment;
          commentType = c;
          start = i+1;
          break;
        default:
          return null;
        }
        break;
        
      case InsideComment:
        switch (c) {
        case '*':
          if (commentType == '*') {
            state = ScanState.InsideCommentStar;
          }
          break;
        case '\n':
        case '\r':
          if (commentType == '/') {
            end = i;
            return src.substring(start,end);
          } else {
            return null; // we don't care about multi-line comments
          }
        }
        
      case InsideCommentStar:
        switch (c) {
        case '/':
          end = i-1;
          return src.substring(start,end);
        case '*':
          break; // remain in same state
        default:
          state = ScanState.InsideComment;
        }
      }
      i++;
    }
    return null; // end of src reached without finding anything interesting
  }
  
  public static Map<String, ArrayList<AstNode>> find(final String src, AstRoot ast) {
    final Map<String, ArrayList<AstNode>> map = new HashMap<String, ArrayList<AstNode>>();
    
    ast.visitAll(new VisitAll() {
      @Override public void handle(AstNode node) {
        if (NameRef.isPrty(node)) {
          String comment = scanComment(src, node.getAbsolutePosition() + node.getLength());
          if (comment == null)
            return;
          comment = comment.trim();
          ArrayList<AstNode> list = map.get(comment);
          if (list == null) {
            list = new ArrayList<AstNode>();
            map.put(comment, list);
          }
          list.add(node);
        }
      }
    });
    
    return map;
  }
}
