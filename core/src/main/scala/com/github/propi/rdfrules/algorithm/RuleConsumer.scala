package com.github.propi.rdfrules.algorithm

import com.github.propi.rdfrules.algorithm.RuleConsumer.{Event, Result}
import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.Rule

import scala.language.implicitConversions

trait RuleConsumer {

  def send(rule: Rule): Unit

  def result: Result

  def withListener(listener: PartialFunction[Event, Unit]): RuleConsumer

  protected def listener: PartialFunction[Event, Unit]

  final protected def invokeEvent(event: Event): Unit = if (listener.isDefinedAt(event)) listener(event)

}

object RuleConsumer {

  trait Event

  trait NoEventRuleConsumer extends RuleConsumer {
    protected def listener: PartialFunction[Event, Unit] = PartialFunction.empty

    protected def withListener(listener: PartialFunction[Event, Unit]): RuleConsumer = this
  }

  trait Invoker[T] extends {
    def invoke(f: RuleConsumer => T)(implicit tripleIndex: TripleIndex[Int], mapper: TripleItemIndex): T
  }

  def apply[T](g: (RuleConsumer => T) => T): Invoker[T] = new Invoker[T] {
    def invoke(f: RuleConsumer => T)(implicit tripleIndex: TripleIndex[Int], mapper: TripleItemIndex): T = g(f)
  }

  def withIndices[T](g: TripleIndex[Int] => TripleItemIndex => (RuleConsumer => T) => T): Invoker[T] = new Invoker[T] {
    def invoke(f: RuleConsumer => T)(implicit tripleIndex: TripleIndex[Int], mapper: TripleItemIndex): T = g(tripleIndex)(mapper)(f)
  }

  def withIndex[T](g: TripleIndex[Int] => (RuleConsumer => T) => T): Invoker[T] = new Invoker[T] {
    def invoke(f: RuleConsumer => T)(implicit tripleIndex: TripleIndex[Int], mapper: TripleItemIndex): T = g(tripleIndex)(f)
  }

  def withMapper[T](g: TripleItemIndex => (RuleConsumer => T) => T): Invoker[T] = new Invoker[T] {
    def invoke(f: RuleConsumer => T)(implicit tripleIndex: TripleIndex[Int], mapper: TripleItemIndex): T = g(mapper)(f)
  }

  case class Result(rules: Traversable[Rule.Simple], isCached: Boolean)

}