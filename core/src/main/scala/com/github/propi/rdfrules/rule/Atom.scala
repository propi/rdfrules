package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.TripleHashIndex.HashSet
import com.github.propi.rdfrules.index.{TripleHashIndex, TripleItemHashIndex}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
sealed trait Atom {
  val subject: Atom.Item
  val predicate: Int
  val `object`: Atom.Item

  def subjectPosition = TripleItemPosition.Subject(subject)

  def objectPosition = TripleItemPosition.Object(`object`)

  def transform(subject: Atom.Item = subject, predicate: Int = predicate, `object`: Atom.Item = `object`): Atom

  def toGraphBasedAtom(implicit thi: TripleHashIndex): Atom.GraphBased = this match {
    case x: Atom.GraphBased => x
    case _: Atom.Basic =>
      val graphs = (subject, `object`) match {
        case (_: Atom.Variable, _: Atom.Variable) => thi.getGraphs(predicate)
        case (_: Atom.Constant, _: Atom.Variable) => thi.getGraphs(predicate, subjectPosition)
        case (_: Atom.Variable, _: Atom.Constant) => thi.getGraphs(predicate, objectPosition)
        case (Atom.Constant(s), Atom.Constant(o)) => thi.getGraphs(s, predicate, o)
      }
      Atom.GraphBased(subject, predicate, `object`)(graphs)
  }
}

object Atom {

  case class Basic private(subject: Atom.Item, predicate: Int, `object`: Atom.Item) extends Atom {
    override def toString: String = s"<$subject $predicate ${`object`}>"

    def transform(subject: Item = subject, predicate: Int = predicate, `object`: Item = `object`): Basic = Basic(subject, predicate, `object`)
  }

  case class GraphBased private(subject: Atom.Item, predicate: Int, `object`: Atom.Item)(graphs: HashSet[Int]) extends Atom {
    def containsGraph(x: Int): Boolean = graphs(x)

    def graphsIterator: Iterator[Int] = graphs.iterator

    def transform(subject: Item = subject, predicate: Int = predicate, `object`: Item = `object`): GraphBased = GraphBased(subject, predicate, `object`)(graphs)

    override def toString: String = {
      def bracketGraphs(strGraphs: String): String = if (graphs.size == 1) strGraphs else s"[$strGraphs]"

      s"<$subject $predicate ${`object`} ${bracketGraphs(graphs.iterator.mkString(", "))}>"
    }
  }

  def apply(subject: Atom.Item, predicate: Int, `object`: Atom.Item): Atom = Basic(subject, predicate, `object`)

  sealed trait Item

  object Item {
    implicit def apply(tripleItem: TripleItem)(implicit mapper: TripleItemHashIndex): Constant = Constant(mapper.getIndex(tripleItem))

    implicit def apply(index: Char): Variable = Variable(index.toInt - 97)

    implicit def apply(string: String): Variable = apply(string.stripPrefix("?").headOption.getOrElse('a'))
  }

  case class Variable(index: Int) extends Item {
    def value: String = {
      val doubleVal = math.abs(index).toDouble
      "?" + Iterator.iterate(math.floor(doubleVal / 26) -> (doubleVal % 26))(x => math.floor(x._1 / 26) -> ((x._1 % 26) - 1))
        .takeWhile(_._2 >= 0)
        .map(x => (97 + x._2).toChar)
        .foldLeft("")((x, y) => y + x)
    }

    def ++ = Variable(index + 1)

    def -- = Variable(index - 1)

    override def toString: String = value
  }

  case class Constant(value: Int) extends Item

  implicit val variableOrdering: Ordering[Variable] = Ordering.by[Variable, Int](_.index)

}
