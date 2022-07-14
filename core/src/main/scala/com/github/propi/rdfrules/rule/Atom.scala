package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.TripleIndex.HashSet
import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
sealed trait Atom {
  def subject: Atom.Item
  def predicate: Int
  def `object`: Atom.Item

  def subjectPosition: TripleItemPosition.Subject[Atom.Item] = TripleItemPosition.Subject(subject)

  def objectPosition: TripleItemPosition.Object[Atom.Item] = TripleItemPosition.Object(`object`)

  def transform(subject: Atom.Item = subject, predicate: Int = predicate, `object`: Atom.Item = `object`): Atom

  def toGraphAwareAtom(implicit thi: TripleIndex[Int]): Atom.GraphAware = this match {
    case x: Atom.GraphAware => x
    case _ =>
      val graphs = (subject, `object`) match {
        case (_: Atom.Variable, _: Atom.Variable) => thi.getGraphs(predicate)
        case (Atom.Constant(x), _: Atom.Variable) => thi.getGraphs(predicate, TripleItemPosition.Subject(x))
        case (_: Atom.Variable, Atom.Constant(x)) => thi.getGraphs(predicate, TripleItemPosition.Object(x))
        case (Atom.Constant(s), Atom.Constant(o)) => thi.getGraphs(s, predicate, o)
      }
      Atom.GraphAwareBasic(subject, predicate, `object`)(graphs)
  }
}

object Atom {

  sealed trait GraphAware extends Atom {
    def graphs: HashSet[Int]

    final def containsGraph(x: Int): Boolean = graphs.contains(x)

    final def graphsIterator: Iterator[Int] = graphs.iterator
  }

  private case class Basic private(subject: Atom.Item, predicate: Int, `object`: Atom.Item) extends Atom {
    override def toString: String = s"<$subject $predicate ${`object`}>"

    def transform(subject: Item = subject, predicate: Int = predicate, `object`: Item = `object`): Basic = Basic(subject, predicate, `object`)
  }

  private case class GraphAwareBasic private(subject: Atom.Item, predicate: Int, `object`: Atom.Item)(val graphs: HashSet[Int]) extends GraphAware {
    def transform(subject: Item = subject, predicate: Int = predicate, `object`: Item = `object`): GraphAware = GraphAwareBasic(subject, predicate, `object`)(graphs)

    override def toString: String = {
      def bracketGraphs(strGraphs: String): String = if (graphs.size == 1) strGraphs else s"[$strGraphs]"

      s"<$subject $predicate ${`object`} ${bracketGraphs(graphs.iterator.mkString(", "))}>"
    }
  }

  def apply(subject: Atom.Item, predicate: Int, `object`: Atom.Item): Atom = Basic(subject, predicate, `object`)

  def apply(subject: Atom.Item, predicate: Int, `object`: Atom.Item, graphs: HashSet[Int]): GraphAware = GraphAwareBasic(subject, predicate, `object`)(graphs)

  sealed trait Item

  object Item {
    implicit def apply(tripleItem: TripleItem)(implicit mapper: TripleItemIndex): Constant = Constant(mapper.getIndex(tripleItem))

    implicit def apply(index: Char): Variable = Variable(index.toInt - 97)

    implicit def apply(string: String): Variable = apply(string.stripPrefix("?").headOption.getOrElse('a'))
  }

  //TODO extends AnyVal
  case class Variable(index: Int) extends Item {
    def value: String = {
      val doubleVal = math.abs(index).toDouble
      "?" + Iterator.iterate(math.floor(doubleVal / 26) -> (doubleVal % 26))(x => math.floor(x._1 / 26) -> ((x._1 % 26) - 1))
        .takeWhile(_._2 >= 0)
        .map(x => (97 + x._2).toChar)
        .foldLeft("")((x, y) => s"$y$x")
    }

    def ++ : Variable = Variable(index + 1)

    def -- : Variable = Variable(index - 1)

    override def toString: String = value
  }

  //TODO extends AnyVal
  case class Constant(value: Int) extends Item

  implicit val variableOrdering: Ordering[Variable] = Ordering.by[Variable, Int](_.index)

}
