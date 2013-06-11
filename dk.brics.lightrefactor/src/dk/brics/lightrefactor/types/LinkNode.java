package dk.brics.lightrefactor.types;

/**
 * Nodes in a circularly linked list
 */
public class LinkNode<T> {
  public T item;
  public LinkNode<T> next = this;
  public LinkNode<T> prev = this;
  
  public LinkNode(T item) {
    this.item = item;
  }
  
  public void splice(LinkNode<T> node) {
    LinkNode<T> x = next;
    LinkNode<T> y = node.next;
    this.next = y;
    y.prev = this;
    node.next = x;
    x.prev = node;
  }
}

