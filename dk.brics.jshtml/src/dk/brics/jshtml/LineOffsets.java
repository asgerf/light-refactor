package dk.brics.jshtml;

import java.util.ArrayList;

public class LineOffsets {
  private ArrayList<Integer> lineOffsets = new ArrayList<Integer>();
  
  public LineOffsets(CharSequence code) {
    lineOffsets.add(0);
    boolean wasR = false;
    for (int i=0; i<code.length(); i++) {
      char ch = code.charAt(i);
      if (ch == '\r') {
        lineOffsets.add(i+1);
        wasR = true;
      }
      else if (ch == '\n') {
        if (wasR) {
          lineOffsets.set(lineOffsets.size()-1, i+1);
        } else {
          lineOffsets.add(i+1);
        }
        wasR = false;
      } else {
        wasR = false;
      }
    }
    // add an extra line break at the end if file does not end with a line break
    if (lineOffsets.get(lineOffsets.size()-1) != code.length()) {
      lineOffsets.add(code.length());
    }
  }
  
  public int getLine(int offset) {
    // binary search
    int low = 0;
    int high = lineOffsets.size()-2;
    while (low <= high) {
      int mid = (low + high) / 2;
      int start = lineOffsets.get(mid);
      int end = lineOffsets.get(mid+1);
      if (offset < start)
        high = mid-1;
      else if (offset > end)
        low = mid+1;
      else
        return mid;
    }
    throw new IllegalArgumentException("Offset out of range");
  }
  public int getStartOfLine(int line) {
    return lineOffsets.get(line);
  }
}
