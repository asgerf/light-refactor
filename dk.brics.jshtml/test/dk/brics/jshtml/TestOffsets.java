package dk.brics.jshtml;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;
import name.fraser.neil.plaintext.diff_match_patch.Patch;

public class TestOffsets {
  private static String readReader(Reader reader) throws IOException {
    try {
      StringBuilder b = new StringBuilder();
      char[] buf = new char[128];
      int len;
      while ((len = reader.read(buf)) != -1) {
        b.append(buf, 0, len);
      }
      reader.close();
      reader = null;
      return b.toString();
    } finally {
      if (reader != null)
        reader.close();
    }
  }
  private static String readFile(File file) throws IOException {
    return readReader(new FileReader(file));
  }
  private static void findFiles(File file, List<File> output) {
    if (file.getName().startsWith("."))
      return;
    if (file.isDirectory()) {
      for (File child : file.listFiles()) {
        findFiles(child, output);
      }
    } else if (file.isFile() && file.getName().endsWith(".html")) {
      output.add(file);
    }
  }
  public static void main(String[] args) throws Exception {
    boolean ok = true;
    File testDir = new File("tests");
    if (args.length >= 1) {
      testDir = new File(args[0]);
    }
    List<File> files = new ArrayList<File>();
    findFiles(testDir, files);
    for (File file : files) {
      String str = readFile(file);
      List<HtmlJs> frags = Html.extract(str);
      for (HtmlJs frag : frags) {
        if (frag instanceof InlineJs) {
          InlineJs inl = (InlineJs)frag;
          String codeFragment = str.substring(inl.getOffset(), inl.getOffset()+inl.getCode().length());
          if (!codeFragment.equals(inl.getCode())) {
            System.err.println("Mismatch in " + file.getName() + " at line " + inl.getLine());
            List<Patch> patches = new diff_match_patch().patch_make(codeFragment, inl.getCode());
            System.err.println(new diff_match_patch().patch_toText(patches));
//            for (Patch diff : diffs) {
//              System.err.println(diff.start1)
//              if (diff.operation != Operation.EQUAL) {
//                System.err.println(diff.text);
//              }
//            }
//            System.err.println(codeFragment + " vs " + inl.getCode());
            ok = false;
          }
        }
      }
    }
    if (ok) {
      System.out.println("OK");
    } else {
      System.exit(1);
    }
  }
}
