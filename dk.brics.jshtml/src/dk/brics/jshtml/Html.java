package dk.brics.jshtml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagNodeVisitor;

/**
 * Static methods for finding JavaScript code in HTML documents.
 */
public class Html {
  public static List<HtmlJs> extract(File file) throws IOException {
    return extract(new FileReader(file));
  }
  
  public static List<HtmlJs> extract(Reader reader) throws IOException {
    try {
      StringBuilder b = new StringBuilder();
      char[] buf = new char[128];
      int len;
      while ((len = reader.read(buf)) != -1) {
        b.append(buf, 0, len);
      }
      reader.close();
      reader = null;
      return extract(b.toString());
    } finally {
      if (reader != null)
        reader.close();
    }
  }
  
  public static List<HtmlJs> extract(final String htmlCode) {
    final LineOffsets offsets = new LineOffsets(htmlCode);
    HtmlCleaner cleaner = new HtmlCleaner();
//    cleaner.getProperties().setKeepWhitespaceAndCommentsInHead(true); // does not seem to make a difference
//    cleaner.getProperties().setAddNewlineToHeadAndBody(true);
    TagNode root = cleaner.clean(htmlCode);
    final List<HtmlJs> result = new ArrayList<HtmlJs>();
    root.traverse(new TagNodeVisitor() {
      Attrs computeAttrs(TagNode node) {
        if (node.getRow() == 0)
          return null;
        int pos = offsets.getStartOfLine(node.getRow()-1) + node.getCol();
        return computeAttributeOffsets(htmlCode, pos);
      }
      @Override
      public boolean visit(TagNode parent, HtmlNode elem) {
        if (!(elem instanceof TagNode))
          return true;
        Attrs attrs = null;
        TagNode node = (TagNode)elem;
        if (node.getName().equals("script") && isJavaScriptType(node.getAttributeByName("type")) && isJavaScriptLanguage(node.getAttributeByName("language"))) {
          if (node.hasAttribute("src")) {
            result.add(new ExternJs(node.getAttributeByName("src"), node.getRow()));
          } else {
            int line = node.getRow()-1; // translate to 0-based
            int col = node.getCol(); // no need to translate here
            int offset = offsets.getStartOfLine(line) + col;
            result.add(new InlineJs(node.getText().toString(), offset, line, col));
          }
        }
        else if (node.getName().equalsIgnoreCase("a") && node.hasAttribute("href") && node.getAttributeByName("href").toLowerCase().trim().startsWith("javascript:")) {
          attrs = computeAttrs(node);
          String href = attrs.value("href");
          int start = href.toLowerCase().indexOf("javascript:") + "javascript:".length();
          String code = href.substring(start);
          int offset = attrs.offset("href") + start;
          int line = offsets.getLine(offset);
          int col = offset - offsets.getStartOfLine(line);
          result.add(new InlineJs(code, offset, line, col));
        }
        // quickly check if we need to compute precise attribute offsets
        if (attrs == null) {
          for (String attr : node.getAttributes().keySet()) {
            if (isEventAttribute(attr)) {
              attrs = computeAttrs(node);
              break;
            }
          }
        }
        if (attrs != null) {
          // check for event attributes
          for (Map.Entry<String,Attr> en : attrs.map.entrySet()) {
            if (isEventAttribute(en.getKey())) {
              Attr attr = en.getValue();
              String code = attr.value;
              int offset = attr.offset;
              int line = offsets.getLine(offset);
              int col = offset - offsets.getStartOfLine(line);
              result.add(new InlineJs(code, offset, line, col));
            }
          }
        }
        return true;
      }
    });
    return result;
  }
  
  private static class Attr {
    String value;
    int offset;
    Attr(String value, int offset) {
      this.value = value;
      this.offset = offset;
    }
  }
  private static class Attrs {
    Map<String,Attr> map = new HashMap<String,Attr>();
    int offset(String attrName) {
      return map.get(attrName).offset;
    }
    String value(String attrName) {
      return map.get(attrName).value;
    }
  }
  private static Attrs computeAttributeOffsets(String code, int endOfOpenTag) {
    final int INIT = 0;         // between attributes
    final int INSIDE_QUOTE = 1; // inside a quoted value
    final int INSIDE_NAME = 2;  // inside an unquoted identifier, but not on the left side of '='
    final int VALUE = 3;        // just read an attribute value, name of a value-less attribute, or name of opening tag
    final int EQUAL = 4;        // just passed an '=' symbol
    final int INSIDE_ATTR = 5;  // inside attribute name
    
    char quote = '\0';
    Attrs result = new Attrs();
    int startOfValue = -1;  // inclusive
    int endOfValue = -1; // exclusive
    int endOfAttrName = -1; // exclusive
    int i = endOfOpenTag-1; // skip the '>'
    int state = INIT;
    loop: 
    while (true) {
      int currentIndex = i;
      char c = code.charAt(currentIndex);
      i -= 1;
      switch (state) {
      case INIT:
        if (c == '/' || c == '>' || Character.isWhitespace(c)) {
          // ignore
        }
        else if (c == '"') {
          endOfValue = currentIndex; // exclude the quote
          quote = '"';
          state = INSIDE_QUOTE;
        }
        else if (c == '\'') {
          endOfValue = currentIndex; // exclude the quote
          quote = '\'';
          state = INSIDE_QUOTE;
        }
        else {
          endOfValue = currentIndex-1; // include the char
          state = INSIDE_NAME;
        }
        break;
        
      case INSIDE_QUOTE:
        if (c == quote) {
          startOfValue = currentIndex+1; // exclude the quote
          state = VALUE;
        } else if (c == '\r' || c == '\n') {
          break loop; // something terrible has happened at the palace!
        }
        break;
        
      case INSIDE_NAME:
        if (c == '<')
          break loop;
        else if (Character.isWhitespace(c)) {
          startOfValue = currentIndex+1; // exclude the space
          state = VALUE;
        }
        else if (c == '=') {
          startOfValue = currentIndex+1; // exclude the space
          state = EQUAL;
        }
        else {
          // do nothing
        }
        break;
        
      case VALUE: // startOfValue, endOfValue are set
        if (c == '<')
          break loop;
        else if (c == '=') {
          state = EQUAL;
        } else if (Character.isWhitespace(c)) {
          // do nothing
        }
        else if (c == '"') { // discard the value
          endOfValue = currentIndex; // exclude the quote
          quote = '"';
          state = INSIDE_QUOTE;
        }
        else if (c == '\'') { // discard the value
          endOfValue = currentIndex; // exclude the quote
          quote = '\'';
          state = INSIDE_QUOTE;
        }
        else { // discard the value
          endOfValue = currentIndex-1; // include the char
          state = INSIDE_NAME; 
        }
        break;
        
      case EQUAL:
        if (Character.isWhitespace(c)) {
          // do nothing
        }
        else if (c == '<')
          break loop; // just bail out if strange code: <=bar
        else {
          endOfAttrName = currentIndex+1; // include the char
          state = INSIDE_ATTR;
        }
        break;
        
      case INSIDE_ATTR: // startOfValue, endOfValue, endOfAttrName are set
        if (c == '<')
          break loop; // just bail out if strange code: <foo=bar
        else if (Character.isWhitespace(c)) {
          int startOfAttrName = currentIndex+1;
          String attrName = code.substring(startOfAttrName, endOfAttrName);
          String value = code.substring(startOfValue, endOfValue);
          result.map.put(attrName, new Attr(value, startOfValue));
          state = INIT;
        }
        else {
          // do nothing
        }
        break;
      
      default: throw new RuntimeException();
      }
    }
    return result;
  }
  
  private static boolean isJavaScriptType(String type) {
    return type == null || type.toLowerCase().contains("javascript");
  }
  private static boolean isJavaScriptLanguage(String lang) {
    return lang == null || lang.toLowerCase().trim().equals("javascript") || lang.toLowerCase().trim().equals("jscript");
  }
  
  public static final String[] EVENT_ATTRIBUTES = new String[] {
    "onabort",
    "onafterprint",
    "onbeforeprint",
    "onbeforeunload",
    "onblur",
    "oncancel",
    "oncanplay",
    "oncanplaythrough",
    "onchange",
    "onclick",
    "onclose",
    "oncontextmenu",
    "oncuechange",
    "ondblclick",
    "ondrag",
    "ondragend",
    "ondragenter",
    "ondragleave",
    "ondragover",
    "ondragstart",
    "ondrop",
    "ondurationchange",
    "onemptied",
    "onended",
    "onerror",
    "onfocus",
    "onhashchange",
    "oninput",
    "oninvalid",
    "onkeydown",
    "onkeypress",
    "onkeyup",
    "online",
    "onload",
    "onloadeddata",
    "onloadedmetadata",
    "onloadstart",
    "onmessage",
    "onmousedown",
    "onmousemove",
    "onmouseout",
    "onmouseover",
    "onmouseup",
    "onmousewheel",
    "onoffline",
    "ononline",
    "onpagehide",
    "onpageshow",
    "onpause",
    "onplay",
    "onplaying",
    "onpopstate",
    "onprogress",
    "onratechange",
    "onreadystatechange",
    "onreset",
    "onresize",
    "onscroll",
    "onseeked",
    "onseeking",
    "onselect",
    "onshow",
    "onstalled",
    "onstorage",
    "onsubmit",
    "onsuspend",
    "ontimeupdate",
    "onunload",
    "onvolumechange",
    "onwaiting"
  };
  public final static Set<String> EVENT_ATTRIBUTE_SET = new HashSet<String>(Arrays.asList(EVENT_ATTRIBUTES));
  
  public static boolean isEventAttribute(String attrName) {
    return EVENT_ATTRIBUTE_SET.contains(attrName); // TODO: replace by startsWith("on")??
  }
}
