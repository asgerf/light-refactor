package dk.brics.jshtml;

/** Reference to an extern JavaScript file */
public class ExternJs extends HtmlJs {
  private String url;
  private int line;
  
  public ExternJs(String url, int line) {
    this.url = url;
    this.line = line;
  }
  
  /**
   * Line containing the script tag referencing the external file.
   * Line numbers are 0-based.
   */
  public int getLine() {
    return line;
  }
  
  /**
   * URL string of the file referenced. This is the contents of the script tag's src attribute.
   */
  public String getURL() {
    return url;
  }
}
