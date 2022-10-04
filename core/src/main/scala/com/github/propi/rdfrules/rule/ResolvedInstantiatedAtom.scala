package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.data.{Triple, TripleItem}
import com.github.propi.rdfrules.index.TripleItemIndex
import com.github.propi.rdfrules.rule.ResolvedAtom.ResolvedItem

import scala.language.implicitConversions

sealed trait ResolvedInstantiatedAtom {
  def subject: TripleItem.Uri

  def predicate: TripleItem.Uri

  def `object`: TripleItem

  def toResolvedAtom: ResolvedAtom

  def toInstantiatedAtom(implicit tripleItemIndex: TripleItemIndex): InstantiatedAtom

  final def toTriple: Triple = Triple(subject, predicate, `object`)
}

object ResolvedInstantiatedAtom {

  private case class Basic(subject: TripleItem.Uri, predicate: TripleItem.Uri, `object`: TripleItem) extends ResolvedInstantiatedAtom {
    def toResolvedAtom: ResolvedAtom = ResolvedAtom(ResolvedItem(subject), predicate, ResolvedItem(`object`))

    def toInstantiatedAtom(implicit tripleItemIndex: TripleItemIndex): InstantiatedAtom = InstantiatedAtom(
      tripleItemIndex.getIndex(subject),
      tripleItemIndex.getIndex(predicate),
      tripleItemIndex.getIndex(`object`)
    )
  }

  def apply(subject: TripleItem.Uri, predicate: TripleItem.Uri, `object`: TripleItem): ResolvedInstantiatedAtom = Basic(subject, predicate, `object`)

  implicit def apply(atom: InstantiatedAtom)(implicit mapper: TripleItemIndex): ResolvedInstantiatedAtom = apply(mapper.getTripleItem(atom.subject).asInstanceOf[TripleItem.Uri], mapper.getTripleItem(atom.predicate).asInstanceOf[TripleItem.Uri], mapper.getTripleItem(atom.`object`))

}
