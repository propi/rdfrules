package eu.easyminer.rdf.rule

import eu.easyminer.rdf.data.TripleItem
import eu.easyminer.rdf.utils.Stringifier

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
case class Atom(subject: Atom.Item, predicate: Int, `object`: Atom.Item) {
  override def toString: String = s"<$subject $predicate ${`object`}>"
}

object Atom {

  sealed trait Item

  case class Variable(index: Int) extends Item {
    def value: String = "?" + Iterator.iterate(math.floor(index.toDouble / 26) -> (index.toDouble % 26))(x => math.floor(x._1 / 26) -> ((x._1 % 26) - 1))
      .takeWhile(_._2 >= 0)
      .map(x => (97 + x._2).toChar)
      .foldLeft("")((x, y) => y + x)

    def ++ = Variable(index + 1)

    def -- = Variable(index - 1)

    override def toString: String = value
  }

  object Variable {
    def apply(index: Char): Variable = Variable(((index.toInt - 97) % 26) + 97)
  }

  case class Constant(value: Int) extends Item

  implicit val variableOrdering: Ordering[Variable] = Ordering.by[Variable, Int](_.index)

  implicit def itemStringifier(implicit item: Int => TripleItem): Stringifier[Item] = {
    case x: Atom.Variable => x.value
    case Atom.Constant(x) => item(x).toString
  }

  implicit def atomStringifier(implicit item: Int => TripleItem): Stringifier[Atom] = (v: Atom) => s"(${Stringifier(v.subject)} ${item(v.predicate).toString} ${Stringifier(v.`object`)})"

}
