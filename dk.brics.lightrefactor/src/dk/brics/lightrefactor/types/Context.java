package dk.brics.lightrefactor.types;

import org.mozilla.javascript.ast.FunctionNode;

/**
 * The context in which a type is bound. A context can be:
 * <li> the toplevel,
 * <li> a function, or
 * <li> none
 */
public final class Context {
  public static final Context TopLevel = new Context();
  public static final Context None = new Context();
  
  private FunctionNode fn;
  
  private Context() {}
  private Context(FunctionNode fn) {
    this.fn = fn;
  }
  
  /** Returns context for the given function, or TopLevel if fn is null */
  public static Context function(FunctionNode fn) {
    return fn == null ? TopLevel : new Context(fn);
  }
  
  /** Null if this is toplevel or none; otherwise the function */
  public FunctionNode getFunction() {
    return fn;
  }
  
  @Override
  public int hashCode() {
    return (fn == null) ? super.hashCode() : fn.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (fn == null) {
      return obj == this;
    } else if (obj instanceof Context) {
      Context ctx = (Context)obj;
      return fn == ctx.fn;
    } else {
      return false;
    }
  }
  
  
}
