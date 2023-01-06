package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.data.TriplePosition.ConceptPosition
import spray.json.DefaultJsonProtocol._
import spray.json._

sealed trait PropertyCardinalities {
  def size: Int

  def domain: Int

  def range: Int

  def target: ConceptPosition

  def targetAverageCadinality: Int

  def targetModeProbability: Double
}

object PropertyCardinalities {
  sealed trait Mapped extends PropertyCardinalities {
    def property: Int

    def resolved(implicit tripleItemIndex: TripleItemIndex): Resolved
  }

  sealed trait Resolved extends PropertyCardinalities {
    def property: TripleItem.Uri
  }

  private case class MappedBasic(property: Int, size: Int, domain: Int, range: Int, target: ConceptPosition, targetAverageCadinality: Int, targetModeProbability: Double) extends Mapped {
    def resolved(implicit tripleItemIndex: TripleItemIndex): Resolved = ResolvedBasic(tripleItemIndex.getTripleItem(property).asInstanceOf[TripleItem.Uri], size, domain, range, target, targetAverageCadinality, targetModeProbability)
  }

  private case class ResolvedBasic(property: TripleItem.Uri, size: Int, domain: Int, range: Int, target: ConceptPosition, targetAverageCadinality: Int, targetModeProbability: Double) extends Resolved

  def apply(property: Int, index: TripleIndex[Int]#PredicateIndex): PropertyCardinalities.Mapped = {
    MappedBasic(property, index.size(false), index.subjects.size, index.objects.size, index.lowerCardinalitySide, index.averageCardinality, index.modeProbability)
  }

  implicit val propertyCardinalitiesWriter: JsonWriter[PropertyCardinalities.Resolved] = (obj: PropertyCardinalities.Resolved) => JsObject(
    "property" -> obj.property.toJson,
    "size" -> obj.size.toJson,
    "domain" -> obj.domain.toJson,
    "range" -> obj.range.toJson,
    "target" -> obj.target.toJson,
    "targetAverageCadinality" -> obj.targetAverageCadinality.toJson,
    "targetModeProbability" -> obj.targetModeProbability.toJson
  )
}