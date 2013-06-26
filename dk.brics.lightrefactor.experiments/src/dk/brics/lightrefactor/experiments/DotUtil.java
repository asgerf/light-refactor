package dk.brics.lightrefactor.experiments;

import java.awt.Desktop;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Static methods that are helpful when generating files in Graphviz dot format.
 */
public class DotUtil {
    static class PipeStream extends Thread {
      private InputStream in;
      private OutputStream out;
      private volatile boolean killed = false;
      public PipeStream(InputStream in, OutputStream out) {
        this.in = in;
        this.out = out;
      }
      @Override
      public void run() {
        try {
          int c;
          while (!killed && (c = in.read()) != -1) {
            out.write(c);
          }
        } catch (Exception e) {
          // eat it
        }
      }
      public void kill() {
        this.killed = true;
        this.interrupt();
      }
    }
    
    public static boolean compileAndOpen(File file) {
      String dotpath = System.getProperty("graphviz");
      if (dotpath == null) {
        System.err.println("Pass -Dgraphviz=FILE to compile graphviz automatically");
        return false; 
      }
      try {
        Process proc = new ProcessBuilder()
          .directory(file.getParentFile())
          .command(dotpath, "-Tpng", "-O", file.getName())
          .start();
        new PipeStream(proc.getErrorStream(), System.err).start();
        int code = proc.waitFor();
        if (code != 0) {
          System.err.println("Graphviz returned " + code);
          return false;
        }
        File outfile = new File(file.getParent(), file.getName() + ".png");
        Desktop.getDesktop().open(outfile);
        return true;
      } catch (Exception e) {
        // Note: We deliberately consume any exceptions! This method should not throw exceptions.
        System.err.println("Could not open Graphviz diagram");
        System.err.println(e.getMessage());
        return false;
      }
    }
    
    /**
     * Escapes a string so it can be used as a label in Graphviz dot.
     */
    public static String escapeLabel(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '"':
                b.append("\\\"");
                break;
            case '\\':
                b.append("\\\\");
                break;
            case '\b':
                b.append("\\b");
                break;
            case '\t':
                b.append("\\t");
                break;
            case '\n':
                b.append("\\n");
                break;
            case '\r':
                b.append("\\r");
                break;
            case '\f':
                b.append("\\f");
                break;
            case '<':
                b.append("\\<");
                break;
            case '>':
                b.append("\\>");
                break;
            case '{':
                b.append("\\{");
                break;
            case '}':
                b.append("\\}");
                break;
            default:
                if (c >= 0x20 && c <= 0x7e) {
                    b.append(c);
                } else {
                    b.append("\\u");
                    String t = Integer.toHexString(c & 0xffff);
                    for (int j = 0; j + t.length() < 4; j++) {
                        b.append('0');
                    }
                    b.append(t);
                }
            }
        }
        return b.toString();
    }
    
    /**
     * Escapes a string so it can be used as a key in Graphviz dot, i.e. a port or node identifier.
     */
    public static String escapeKey(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                b.append(c);
            } else {
                b.append("__u");
                String t = Integer.toHexString(c & 0xffff);
                for (int j = 0; j + t.length() < 4; j++) {
                    b.append('0');
                }
                b.append(t);
            }
        }
        return b.toString();
    }
}
