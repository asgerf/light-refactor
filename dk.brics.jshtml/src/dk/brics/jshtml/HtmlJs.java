package dk.brics.jshtml;

/** 
 * A piece of HTML containing or referencing JavaScript code. 
 * @see ExternJs
 * @see InlineJs
 */
public abstract class HtmlJs {
  /** Line number */
  public abstract int getLine();
}
