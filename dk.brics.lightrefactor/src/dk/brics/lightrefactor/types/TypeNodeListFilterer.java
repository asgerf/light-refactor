package dk.brics.lightrefactor.types;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class TypeNodeListFilterer {
  
  private TypeNode[] buffer = new TypeNode[64];
  
  public static final class TypeNodeComparator implements Comparator<TypeNode> {
    @Override
    public int compare(TypeNode o1, TypeNode o2) {
      return o1.id - o2.id;
    }
  }
  public static final TypeNodeComparator TypeNodeComparator = new TypeNodeComparator();
  
  public void filter(ArrayList<TypeNode> list) {
    // WARN: Not guaranteed to remove duplicates
    int numStale = 0;
    for (int i=0; i<list.size(); i++) {
      TypeNode t = list.get(i);
      if (t.parent != t) { // is t is stale?
        if (numStale == buffer.length) { // resize buffer if necessary
          buffer = Arrays.copyOf(buffer, buffer.length*2);
        }
        buffer[numStale] = t.rep();
        numStale++;
      } else if (numStale > 0) {
        list.set(i - numStale, t); // overwrite previous stale elements
      }
    }
    if (numStale == 0)
      return;
    Arrays.sort(buffer, 0, numStale, TypeNodeComparator);
    int insertPos = list.size() - numStale;
    for (int i=0; i<numStale; i++) {
      if (i > 0 && buffer[i] == buffer[i-1])
        continue; // skip duplicates
      list.set(insertPos, buffer[i]);
      insertPos++;
    }
    while (insertPos < list.size()) {
      list.remove(list.size()-1);
    }
  }
}
