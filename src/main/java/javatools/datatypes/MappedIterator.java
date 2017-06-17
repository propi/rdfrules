package javatools.datatypes;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;

/** 
This class is part of the Java Tools (see http://mpii.de/yago-naga/javatools).
It is licensed under the Creative Commons Attribution License 
(see http://creativecommons.org/licenses/by/3.0) by 
the YAGO-NAGA team (see http://mpii.de/yago-naga).

  The class wraps an iterator and translates each element before returning it  */

public class MappedIterator<S,T> implements Iterator<T>, Iterable<T>, Closeable {
   protected Iterator<S> iterator;
   protected Map<? super S,? extends T> map;
   public static interface Map<A,B> {
     public B map(A a);
   }
   /** An iterator that maps an object to a string*/
   public static MappedIterator.Map<Object, String> stringMapper = new MappedIterator.Map<Object, String>() {
     public String map(Object a) {
       return a.toString();
     }
   };
   public MappedIterator(Iterator<S> i, Map<? super S,? extends T> m) {
     iterator=i;
     map=m;
   }
  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }
  @Override
  public T next() {
    return(map.map(iterator.next()));
  }
  @Override
  public void remove() {
    iterator.remove();
  }
  @Override
  public Iterator<T> iterator() {
    return this;
  }
  @Override
  public void close() throws IOException {
    if(iterator instanceof Closeable) ((Closeable)iterator).close();
  }
}
