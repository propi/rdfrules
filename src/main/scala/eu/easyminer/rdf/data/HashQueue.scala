package eu.easyminer.rdf.data

/**
  * Created by Vaclav Zeman on 20. 6. 2017.
  */
class HashQueue[T] {

  private val queue = collection.mutable.LinkedHashSet.empty[T]

  def add(item: T) = {
    queue += item
    this
  }

  def peek = queue.head

  def poll = {
    val x = peek
    queue -= x
    x
  }

  def isEmpty = queue.isEmpty

  def size = queue.size

}
