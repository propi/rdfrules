package eu.easyminer.rdf.data

/**
  * Created by Vaclav Zeman on 20. 6. 2017.
  */
class HashQueue[T] {

  private val queue = collection.mutable.LinkedHashSet.empty[T]

  def add(item: T): HashQueue[T] = {
    queue += item
    this
  }

  def peek: T = queue.head

  def poll: T = {
    val x = peek
    queue -= x
    x
  }

  def isEmpty: Boolean = queue.isEmpty

  def size: Int = queue.size

}
