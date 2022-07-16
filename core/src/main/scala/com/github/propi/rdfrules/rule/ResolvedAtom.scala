package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule
import com.github.propi.rdfrules.rule.ResolvedAtom.ResolvedItem

import scala.language.implicitConversions

sealed trait ResolvedAtom {
  def subject: ResolvedItem
  def predicate: TripleItem.Uri
  def `object`: ResolvedItem

  def toAtom(implicit tripleItemIndex: TripleItemIndex): Atom

  override def equals(obj: scala.Any): Boolean = obj match {
    case x: ResolvedAtom => (this eq x) || (subject == x.subject && predicate == x.predicate && `object` == x.`object`)
    case _ => false
  }
}

object ResolvedAtom {

  sealed trait GraphAware extends ResolvedAtom {
    def graphs: Set[TripleItem.Uri]
  }

  sealed trait ResolvedItem {
    def toItem(implicit tripleItemIndex: TripleItemIndex): Atom.Item
  }

  object ResolvedItem {

    case class Variable private(value: String) extends ResolvedItem {
      def toItem(implicit tripleItemIndex: TripleItemIndex): Atom.Item = Atom.Item(value)
    }

    case class Constant private(tripleItem: TripleItem) extends ResolvedItem {
      def toItem(implicit tripleItemIndex: TripleItemIndex): Atom.Item = Atom.Item(tripleItem)
    }

    def apply(char: Char): ResolvedItem = Variable("?" + char)

    def apply(variable: String): ResolvedItem = Variable(variable)

    def apply(tripleItem: TripleItem): ResolvedItem = Constant(tripleItem)

    implicit def apply(atomItem: rule.Atom.Item)(implicit mapper: TripleItemIndex): ResolvedItem = atomItem match {
      case x: rule.Atom.Variable => apply(x.value)
      case rule.Atom.Constant(x) => apply(mapper.getTripleItem(x))
    }
  }

  private case class Basic private(subject: ResolvedItem, predicate: TripleItem.Uri, `object`: ResolvedItem) extends ResolvedAtom {
    def toAtom(implicit tripleItemIndex: TripleItemIndex): Atom = rule.Atom(subject.toItem, tripleItemIndex.getIndex(predicate), `object`.toItem)
  }

  private case class GraphAwareBasic private(subject: ResolvedItem, predicate: TripleItem.Uri, `object`: ResolvedItem)(val graphs: Set[TripleItem.Uri]) extends GraphAware {
    def toAtom(implicit tripleItemIndex: TripleItemIndex): Atom = rule.Atom(subject.toItem, tripleItemIndex.getIndex(predicate), `object`.toItem, graphs.map(tripleItemIndex.getIndex))
  }

  def apply(subject: ResolvedItem, predicate: TripleItem.Uri, `object`: ResolvedItem): ResolvedAtom = Basic(subject, predicate, `object`)

  def apply(subject: ResolvedItem, predicate: TripleItem.Uri, `object`: ResolvedItem, graphs: Set[TripleItem.Uri]): GraphAware = GraphAwareBasic(subject, predicate, `object`)(graphs)

  implicit def apply(atom: rule.Atom)(implicit mapper: TripleItemIndex): ResolvedAtom = atom match {
    case x: rule.Atom.GraphAware =>
      GraphAwareBasic(atom.subject, mapper.getTripleItem(atom.predicate).asInstanceOf[TripleItem.Uri], atom.`object`)(x.graphsIterator.map(mapper.getTripleItem(_).asInstanceOf[TripleItem.Uri]).toSet)
    case _ =>
      apply(atom.subject, mapper.getTripleItem(atom.predicate).asInstanceOf[TripleItem.Uri], atom.`object`)
  }

}