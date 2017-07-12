package eu.easyminer.rdf.data

/**
  * Created by Vaclav Zeman on 27. 6. 2017.
  */
class IncrementalInt {

  private var value = 0

  def ++ = this += 1

  def +=(x: Int) = {
    value += x
    this
  }

  def getValue = value

}

object IncrementalInt {

  def apply(): IncrementalInt = new IncrementalInt

  def apply(value: Int): IncrementalInt = apply += value

}
