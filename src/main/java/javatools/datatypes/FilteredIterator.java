package javatools.datatypes;

import java.util.Iterator;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).

  The class wraps an iterator and returns only those elements that fulfill a condition */

public class FilteredIterator<T> extends PeekIterator<T> {

  protected Iterator<T> iterator;

  protected If<T> condition;

  public static interface If<T> {

    public boolean condition(T a);
  }

  public FilteredIterator(Iterator<T> i, If<T> condition) {
    this(i);
    this.condition = condition;
  }

  protected FilteredIterator(Iterator<T> i) {
    iterator = i;
  }

  @Override
  public T internalNext() {
    while (iterator.hasNext()) {
      T obj = iterator.next();
      if (condition.condition(obj)) return (obj);
    }
    return (null);
  }

  @Override
  public void remove() {
    iterator.remove();
  }

  /** Wraps an iterator and skips elements that produce an exception*/
  public static class IgnoreErrors<T> extends FilteredIterator<T> {

    int numFail;

    /** Wraps the iterator, allowing a number of consecutive failures before returning hasNext()==false */
    public IgnoreErrors(Iterator<T> i, int numFailuresBeforeStop) {
      super(i);
      numFail = numFailuresBeforeStop;
    }

    @Override
    public T internalNext() {
      if (!iterator.hasNext()) return (null);
      int counter = numFail;
      while (counter-- > 0) {
        try {
          return (iterator.next());
        } catch (Exception e) {
        }
      }
      return (null);
    }
  }
}
