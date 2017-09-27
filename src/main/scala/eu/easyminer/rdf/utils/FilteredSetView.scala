package eu.easyminer.rdf.utils

/**
  * Created by Vaclav Zeman on 27. 9. 2017.
  */
class FilteredSetView[A] private(set: collection.Set[A], f: A => Boolean) extends collection.Set[A] {
  def contains(elem: A): Boolean = f(elem) && set(elem)

  def +(elem: A): collection.Set[A] = new FilteredSetView(set + elem, f)

  def -(elem: A): collection.Set[A] = new FilteredSetView(set - elem, f)

  def iterator: Iterator[A] = set.iterator.filter(f)
}

object FilteredSetView {

  def apply[A](f: A => Boolean)(set: collection.Set[A]) = new FilteredSetView(set, f)

}
