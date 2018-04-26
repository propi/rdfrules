package com.github.propi.rdfrules.ruleset

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.TripleItemHashIndex
import com.github.propi.rdfrules.rule
import com.github.propi.rdfrules.rule.{Measure, Rule}
import com.github.propi.rdfrules.ruleset.ResolvedRule.Atom
import com.github.propi.rdfrules.stringifier.Stringifier
import com.github.propi.rdfrules.utils.TypedKeyMap
import com.github.propi.rdfrules.stringifier.CommonStringifiers._

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 17. 4. 2018.
  */
case class ResolvedRule(body: IndexedSeq[Atom], head: Atom)(val measures: TypedKeyMap.Immutable[Measure]) {
  def ruleLength: Int = body.length + 1

  override def toString: String = Stringifier(this)
}

object ResolvedRule {

  case class Atom(subject: Atom.Item, predicate: TripleItem.Uri, `object`: Atom.Item)

  object Atom {

    sealed trait Item

    object Item {

      case class Variable(variable: rule.Atom.Variable) extends Item

      case class Constant(tripleItem: TripleItem) extends Item

      implicit def apply(atomItem: rule.Atom.Item)(implicit mapper: TripleItemHashIndex): Item = atomItem match {
        case x: rule.Atom.Variable => Variable(x)
        case rule.Atom.Constant(x) => Constant(mapper.getTripleItem(x))
      }

    }

    implicit def apply(atom: rule.Atom)(implicit mapper: TripleItemHashIndex): Atom = Atom(
      atom.subject,
      mapper.getTripleItem(atom.predicate).asInstanceOf[TripleItem.Uri],
      atom.`object`
    )

  }

  implicit def apply(rule: Rule)(implicit mapper: TripleItemHashIndex): ResolvedRule = ResolvedRule(
    rule.body.map(Atom.apply),
    rule.head
  )(rule.measures)

}