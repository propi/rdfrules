package javatools.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;

import javatools.administrative.Announce;

/** 
 This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
 It is licensed under the Creative Commons Attribution License 
 (see http://creativecommons.org/licenses/by/3.0) by 
 the YAGO-NAGA team (see http://mpii.de/yago-naga).
 
 
 


 This class can read characters from a file that is UTF8 encoded.<BR>
 Example:
 <PRE>
 Reader f=new UTF8Reader(new File("blah.blb"));
 int c;
 while((c=f.read())!=-1) System.out.print(Char.normalize(c));
 f.close();
 </PRE>
 */
public class NewUTF8Reader extends Reader {

  /** Holds the input Stream */
  private InputStreamReader in;

  /** Buffered input stream*/
  protected BufferedReader bin;

  /** tells whether we want a progress bar*/
  protected boolean progressBar = false;

  /** number of chars for announce */
  protected long numBytesRead = 0;

  /** the buffer that is readinto, */
  private char[] buffer;

  /** the buffer size that is read */
  private int maxBufferLength = 1024;

  /** current position on the buffer when returning */
  private int position = 0;

  /** how many char were read into the buffer*/
  private int readTo = -1;

  /** Constructs a UTF8Reader from a Reader */
  public NewUTF8Reader(InputStream s) {
    try {
      in = new InputStreamReader(s, "UTF8");
      bin = new BufferedReader(in);
    } catch (UnsupportedEncodingException une) {
      une.printStackTrace();
    }
  }

  /** Constructs a UTF8Reader for an URL 
   * @throws IOException */
  public NewUTF8Reader(URL url) throws IOException {
    this(url.openStream());
  }

  /** Constructs a UTF8Reader from a File */
  public NewUTF8Reader(File f) throws FileNotFoundException {
    this(new FileInputStream(f));
  }

  /** Constructs a UTF8Reader from a File, makes a nice progress bar, but reads slower, 1GB in 18 seconds with progressbar, 11 seconds without*/
  public NewUTF8Reader(File f, String message) throws FileNotFoundException {
    this(new FileInputStream(f));
    progressBar = true;
    Announce.progressStart(message, f.length());
  }

  /** Constructs a UTF8Reader from a File */
  public NewUTF8Reader(String f) throws FileNotFoundException {
    this(new File(f));
  }

  /** Constructs a UTF8Reader from a File, makes a nice progress bar, but reads slower, 1GB in 18 seconds with progressbar, 11 seconds without*/
  public NewUTF8Reader(String f, String message) throws FileNotFoundException {
    this(new File(f), message);
  }

  @Override
  public void close() throws IOException {
    if (bin != null) {
      bin.close();
      bin = null;
    }
    if (in != null) {
      in.close();
      in = null;
    }
    if (progressBar) Announce.progressDone();
    progressBar = false;
  }

  @Override
  public int read(char[] cbuf, int off, int len) throws IOException {
    return bin.read(cbuf, off, len);
  }

  @Override
  public int read() throws IOException {
    if (buffer == null || position >= readTo) {
      buffer = new char[maxBufferLength];
      readTo = bin.read(buffer);
      position = 0;
      if (readTo == -1) {
        return -1;
      } else if (progressBar) {
        String line = new String(buffer);
        numBytesRead = numBytesRead + line.getBytes("UTF-8").length;
        Announce.progressAt(numBytesRead);
      }
    }
    int c = buffer[position];
    position++;
    return c;
  }

  /** Reads a line do not between read() and readLine methods pick one and stick to it.*/
  public String readLine() throws IOException {
    String line = bin.readLine();
    if (progressBar) {
      if (line != null) {
        numBytesRead += line.getBytes().length;
        Announce.progressAt(numBytesRead);
      }
    }
    return line;
  }

  /** Returns the number of bytes read from the underlying stream*/
  public long numBytesRead() {
    return (numBytesRead);
  }

  /** Test method
   * @throws IOException   */
  public static void main(String[] args) throws IOException {
    for (int i = 0; i < 5; i++) {
      long time = System.currentTimeMillis();
      String file = "C:/yagoTest/yago2/wasFoundIn.tsv";
      //    String file = "C:/wikipedia/enwiki-100-test.xml";
      NewUTF8Reader ff = new NewUTF8Reader(new File(file), "test");
      long count = 0;
      while (ff.read() != -1) {
        count++;
      }
      ff.close();
      time = System.currentTimeMillis() - time;
      System.out.println("Read "+count+" chars in " + time);
    }
  }
}
