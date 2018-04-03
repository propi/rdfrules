package com.github.propi.rdfrules.rule

import com.github.propi.rdfrules.utils.TypedKeyMap

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 16. 6. 2017.
  */
trait Rule {

  val body: IndexedSeq[Atom]
  val head: Atom
  val measures: TypedKeyMap.Immutable[Measure]

  lazy val ruleLength: Int = body.size + 1

  override def hashCode(): Int = {
    val support = measures[Measure.Support].value
    val headSize = measures[Measure.HeadSize].value
    val bodyHashCode = body.iterator.map { atom =>
      atom.predicate +
        (atom.subject match {
          case constant: Atom.Constant => constant.value
          case _ => 0
        }) +
        (atom.`object` match {
          case constant: Atom.Constant => constant.value
          case _ => 0
        })
    }.foldLeft(0)(_ ^ _)
    bodyHashCode + body.size * headSize + support
  }

}

object Rule {

  case class Simple(head: Atom, body: IndexedSeq[Atom])(val measures: TypedKeyMap.Immutable[Measure]) extends Rule

  object Simple {
    implicit def apply(extendedRule: ExtendedRule): Simple = new Simple(extendedRule.head, extendedRule.body)(extendedRule.measures)
  }

  //implicit def rulePrinter(implicit str: Stringifier[Rule]): Printer[Rule] = (v: Rule) => println(Stringifier(v))

}