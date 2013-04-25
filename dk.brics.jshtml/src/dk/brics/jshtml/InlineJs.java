package dk.brics.jshtml;

/**
 * A piece of JavaScript code inlined in an HTML file.
 */
public class InlineJs extends HtmlJs {
  private final String code;
  private final int offset;
  private final int line;
  private final int column;
  
  public InlineJs(String code, int offset, int line, int column) {
    this.code = code;
    this.offset = offset;
    this.line = line;
    this.column = column;
  }
  
  /** Source code of this fragment */
  public String getCode() {
    return code;
  }
  
  /** 
   * Absolute position in file.
   * This is a 0-based index of the first character in this fragment, relative to the start of the file.
   */
  public int getOffset() {
    return offset;
  }

  /** 
   * Line number.
   * This is a 0-based index of the line containing the first character of this fragment.
   */
  public int getLine() {
    return line;
  }

  /** 
   * Column number.
   * This is a 0-based index of the first character in this fragment, relative to the start of the line.
   * <p/>
   * Note that a TAB character counts as a single character, which may not reflect how editors display column numbers. 
   */
  public int getColumn() {
    return column;
  }
}
