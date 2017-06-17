package javatools.datatypes;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).
  

  
 

  The class combines multiple iterables to one iterable.
  This can be used in a for-each-loop.<BR>
  Example:
   <PRE>
         for(Object o : new CombinedIterable(list1,list2))
               process(o);
   </PRE>
  */
public class CombinedIterable<T> implements  Iterable<T>, Closeable {
  /** Holds the queue of iterables */
  private Queue<Iterable<? extends T>> iterables=new LinkedList<Iterable<? extends T>>();
  /** Creates an empty CombinedIterator */
  public CombinedIterable() {
  }
  /** Creates a CombinedIterator two iterators */
  public CombinedIterable(Iterable<? extends T> i1, Iterable<? extends T> i2) {
    iterables.offer(i1);
    iterables.offer(i2);
  }  
  /** Creates a CombinedIterator from one iterator */
  public CombinedIterable(Iterable<? extends T> i1) {
    iterables.offer(i1);
  }  
  /** Creates a CombinedIterator three iterators */
  public CombinedIterable(Iterable<? extends T> i1, Iterable<? extends T> i2, Iterable<? extends T> i3) {
    iterables.offer(i1);
    iterables.offer(i2);
    iterables.offer(i3);
  }  
  /** Adds a set */ 
  public CombinedIterable(T i) {
    this(Arrays.asList(i));
  }
  /** Creates a CombinedIterator from some iterators (may give a (useless) Java compiler warning)*/
  @SuppressWarnings("unchecked")
  public CombinedIterable(Iterable<? extends T>... its) {
    for(Iterable<? extends T> i : its) iterables.offer(i);
  }
  /** Adds an iterable */
  public CombinedIterable<T> add(Iterable<? extends T> i) {
    iterables.offer(i);
    return(this);
  }
  /** Adds a value */  
  public CombinedIterable<T> add(T i) {
    return(add(Arrays.asList(i)));
  }
  /** Returns this */
  public Iterator<T> iterator() {
   return(new CombinedIterator<T>(iterables));
  }
  
  @Override
  public String toString() {
    StringBuilder b=new StringBuilder("[");
    for(T t : this) b.append(t).append(", ");
    if(b.length()>2) b.setLength(b.length()-2);
    b.append("]");
    return(b.toString());
  }
  @Override
  public void close() throws IOException {
    for(Iterable<? extends T> i : iterables) {
      if(i instanceof Closeable) ((Closeable)i).close();
    }
    iterables.clear();
  }
}
