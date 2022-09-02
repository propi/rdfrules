package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.TripleItem

sealed trait PropertyCardinalities {
  def size: Int

  def domain: Int

  def range: Int
}

object PropertyCardinalities {
  sealed trait Mapped extends PropertyCardinalities {
    def property: Int
    def resolved(implicit tripleItemIndex: TripleItemIndex): Resolved
  }

  sealed trait Resolved extends PropertyCardinalities {
    def property: TripleItem.Uri
  }

  private case class MappedBasic(property: Int, size: Int, domain: Int, range: Int) extends Mapped {
    def resolved(implicit tripleItemIndex: TripleItemIndex): Resolved = ResolvedBasic(tripleItemIndex.getTripleItem(property).asInstanceOf[TripleItem.Uri], size, domain, range)
  }

  private case class ResolvedBasic(property: TripleItem.Uri, size: Int, domain: Int, range: Int) extends Resolved

  def apply(property: Int, index: TripleIndex[Int]#PredicateIndex): PropertyCardinalities.Mapped = {
    MappedBasic(property, index.size(false), index.subjects.size, index.objects.size)
  }
}