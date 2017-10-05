package eu.easyminer.rdf.utils

/**
  * Created by Vaclav Zeman on 20. 6. 2017.
  */
class Stack[T](a: T*) {

  private var list: List[T] = a.toList

  def peek: T = list.head

  def pop: T = {
    val head = peek
    list = list.tail
    head
  }

  def isEmpty: Boolean = list.isEmpty

  def push(v: T): Unit = list = v :: list

}
