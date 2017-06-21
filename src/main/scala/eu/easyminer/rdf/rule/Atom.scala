package eu.easyminer.rdf.rule

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
case class Atom(subject: Atom.Item, predicate: String, `object`: Atom.Item) {
  override def toString: String = s"<$subject $predicate ${`object`}>"
}

object Atom {

  sealed trait Item

  case class Variable(index: Int) extends Item {
    def value = "?" + Iterator.iterate(math.floor(index.toDouble / 26) -> (index.toDouble % 26))(x => math.floor(x._1 / 26) -> ((x._1 % 26) - 1))
      .takeWhile(_._2 >= 0)
      .map(x => (97 + x._2).toChar)
      .foldLeft("")((x, y) => y + x)

    def ++ = Variable(index + 1)

    def -- = Variable(index - 1)

    override def toString: String = value
  }

  implicit val variableOrdering = Ordering.by[Variable, Int](_.index)

  case class Constant(value: String) extends Item

}
