package javatools.datatypes;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).
  

  
 

  The class combines multiple iterators to one iterator.
  The nice thing about it:  The object is an Iterator as well as an Iterable,
  i.e. it can be used in a for-each-loop.<BR>
  Example:
   <PRE>
         for(Object o : new CombinedIterator(list1.iterator(),list2.iterator()))
               process(o);
   </PRE>
  */
public class CombinedIterator<T> implements Iterator<T>, Iterable<T>, Closeable {

  /** Holds the queue of iterators */
  private Queue<Iterator<? extends T>> iterators = new LinkedList<Iterator<? extends T>>();

  /** Creates an empty CombinedIterator */
  public CombinedIterator() {
  }

  /** Creates a CombinedIterator two iterators */
  public CombinedIterator(Iterator<? extends T> i1, Iterator<? extends T> i2) {
    iterators.offer(i1);
    iterators.offer(i2);
  }

  /** Creates a CombinedIterator from one iterator */
  public CombinedIterator(Iterator<? extends T> i1) {
    iterators.offer(i1);
  }

  /** Creates a CombinedIterator three iterators */
  public CombinedIterator(Iterator<? extends T> i1, Iterator<? extends T> i2, Iterator<? extends T> i3) {
    iterators.offer(i1);
    iterators.offer(i2);
    iterators.offer(i3);
  }

  /** Adds a set */
  public CombinedIterator(Iterable<? extends T> i) {
    this(i.iterator());
  }

  /** Adds a set */
  @SuppressWarnings("unchecked")
  public CombinedIterator(T i) {
    this(Arrays.asList(i));
  }

  /** Creates a CombinedIterator from some iterators (may give a (useless) Java compiler warning)*/
  public CombinedIterator(Iterator<? extends T>... its) {
    for (Iterator<? extends T> i : its)
      iterators.offer(i);
  }

  /** Creates a CombinedIterator from some iterators */
  public CombinedIterator(Collection<Iterable<? extends T>> its) {
    for (Iterable<? extends T> i : its)
      iterators.offer(i.iterator());
  }

  /** Adds an iterator */
  public CombinedIterator<T> add(Iterator<? extends T> i) {
    iterators.offer(i);
    return (this);
  }

  /** Adds a value */
  @SuppressWarnings("unchecked")
  public CombinedIterator<T> add(T i) {
    return (add(Arrays.asList(i)));
  }

  /** Adds a set */
  public CombinedIterator<T> add(Iterable<? extends T> i) {
    iterators.offer(i.iterator());
    return (this);
  }

  /** TRUE if there are more elements */
  public boolean hasNext() {
    if (iterators.peek() == null) return (false);
    if (iterators.peek().hasNext()) return (true);
    if (iterators.peek() instanceof Closeable) {
      try {
        ((Closeable) iterators.peek()).close();
      } catch (Exception e) {
      }
    }
    iterators.remove();
    return (hasNext());
  }

  /** Returns next */
  public T next() {
    if (!hasNext()) return (null);
    return (iterators.peek().next());
  }

  /** Returns this */
  public Iterator<T> iterator() {
    return (this);
  }

  /** Removes the current item*/
  public void remove() {
    iterators.peek().remove();
  }

  /** Closes all iterators (if closeable)*/
  public void close() {
    while (!iterators.isEmpty()) {
      Iterator<? extends T> i = iterators.poll();
      if (i instanceof Closeable) {
        try {
          ((Closeable) i).close();
        } catch (Exception e) {
        }
      }
    }
  }

}
