package dk.brics.lightrefactor.types;

import java.util.Collection;
import java.util.Set;

import org.mozilla.javascript.ast.AstNode;
import org.mozilla.javascript.ast.Scope;

/**
 * The typing of a program.
 */
public interface Typing {
  /**
   * Type of the global object.
   */
  TypeNode global();
  
  /**
   * Returns the type of the given AST node.
   * Valid AST nodes are: expressions, functions.
   */
  TypeNode typ(AstNode node);

  /**
   * Returns the type of the given variable in the given scope,
   * or <tt>null</tt> if the scope has no such variable.
   * <p/>
   * <b>Special variable names:</b>
   * <ul>
   * <li><tt>@this</tt>: the type of <tt>this</tt> (only valid for function scopes)
   * <li><tt>@return</tt>: the return type (only valid for function scopes)
   * </ul>
   */
  TypeNode lookupVar(Scope scope, String varName);
  
  /**
   * Like {@link #lookupVar(Scope, String)} but creates a type for the variable if it did not exist.
   */
  TypeNode getVar(Scope scope, String varName);
  
  Set<String> variables(Scope scope);
  
  /**
   * Returns the type of the given property, or <tt>null</tt> if the type does not have
   * the given property.
   */
  TypeNode lookupPrty(TypeNode type, String prty);
  
  /**
   * Returns the type of the given property, creating the property if it did not exist.
   * This modifies the typing of the program. Future queries to this property will return
   * the same type.
   */
  TypeNode getPrty(TypeNode type, String prty);
  
  Set<String> properties(TypeNode type);
  
  Iterable<TypeNode> superTypes(TypeNode type);
  Iterable<TypeNode> subTypes(TypeNode type);
  
  Iterable<TypeNode> rootTypes();
  
}
