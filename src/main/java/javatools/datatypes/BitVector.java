package javatools.datatypes;


import java.util.AbstractSet;
import java.util.Arrays;


import javatools.administrative.D;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).
   
This class implements an efficient boolean (bit) list . It wraps BitSet into a set of Integers
<BR>
*/

public class BitVector extends AbstractSet<Integer> {

  protected long[] data;

  protected int size = 0;

  public BitVector(int capacity) {
    data = new long[capacity / 64 + 1];
  }

  public BitVector(BitVector v) {
    data = Arrays.copyOf(v.data, v.data.length);
    size = v.size;
  }

  public BitVector() {
    this(10);
  }

  protected void ensureCapacity(int c) {
    if (data.length * 64 > c) return;
    long[] newdta = new long[c / 64 + 1];
    System.arraycopy(data, 0, newdta, 0, data.length);
    data = newdta;
  }

  @Override
  public boolean add(Integer pos) {
    return (add(pos.intValue()));
  }

  public boolean add(int pos) {
    ensureCapacity(pos);
    if (contains(pos)) return (false);
    long add = 1L << (pos % 64);
    data[pos / 64] |= add;
    size++;
    return (true);
  }

  @Override
  public boolean remove(Object pos) {
    if (!(pos instanceof Integer)) return (false);
    return (remove(((Integer) pos).intValue()));
  }

  public boolean remove(int pos) {
    if (!contains(pos)) return (false);
    long add = 1L << (pos % 64);
    data[pos / 64] &= ~add;
    size--;
    return (true);
  }

  @Override
  public boolean contains(Object arg0) {
    if (!(arg0 instanceof Integer)) return (false);
    return contains(((Integer) arg0).intValue());
  }

  public boolean contains(int index) {
    if (index/64 >= data.length) return (false);
    long l = data[index / 64];
    return (l >> (index % 64) & 1) > 0;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public void clear() {
    data = new long[1];
    size = 0;
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder(size * 5).append("[");
    for (Integer i : this)
      b.append(i).append(", ");
    return (b.append("]").toString());
  }

  public void compress() {
    for(int i=data.length-1;i>=0;i--) {
      if(data[i]!=0) {
        if(i<data.length-10) {
          long[] newdta = new long[i+1];
          System.arraycopy(data, 0, newdta, 0, newdta.length);
          data = newdta;          
        }
        return;
      }
    }
  }
  
  @Override
  public PeekIterator<Integer> iterator() {
    compress();
    return new PeekIterator<Integer>() {

      int i = -1;

      @Override
      protected Integer internalNext() throws Exception {
        while (true) {
          if(i/64>=data.length) return(null);
          if(i>=0 && data[i/64]==0) i+=64;
          else i++;
          if (contains(i)) return (i);
        }
      }

    };
  }

  /**  Test */
  public static void main(String[] args) {
    BitVector v = new BitVector();
    v.add(1);
    v.add(63);
    v.add(64);
    v.add(10000000);
    v.remove(10);
    v.remove(63);
    D.p(v);
  }

}
