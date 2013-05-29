package dk.brics.jshtml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Dump {
  public static void main(String[] args) throws IOException {
    List<File> files = new ArrayList<File>();
    for (String arg : args) {
      files.add(new File(arg));
    }
    for (File file : files) {
      List<HtmlJs> frags = Html.extract(file);
      for (HtmlJs frag : frags) {
        if (frag instanceof InlineJs) {
          InlineJs inl = (InlineJs)frag;
          System.out.printf("%s:%d:%d\n", file.getName(), inl.getLine(), inl.getColumn());
          System.out.print(inl.getCode());
          System.out.println();
        } else {
          ExternJs ext = (ExternJs)frag;
          System.out.printf("%s:%d ->%s\n", file.getName(), ext.getLine(), ext.getURL());
        }
      }
    }
  }
}
