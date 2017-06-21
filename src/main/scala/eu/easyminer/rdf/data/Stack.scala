package eu.easyminer.rdf.data

/**
  * Created by Vaclav Zeman on 20. 6. 2017.
  */
class Stack[T](a: T*) {

  private var list: List[T] = a.toList

  def peek = list.head

  def pop = {
    val head = peek
    list = list.tail
    head
  }

  def isEmpty: Boolean = list.isEmpty

  def push(v: T) = list = v :: list

}
