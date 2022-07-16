package com.github.propi.rdfrules.algorithm

import com.github.propi.rdfrules.algorithm.RuleConsumer.Event
import com.github.propi.rdfrules.index.{TripleIndex, TripleItemIndex}
import com.github.propi.rdfrules.rule.Rule
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.utils.ForEach

import scala.language.implicitConversions

trait RuleConsumer {

  @volatile private var _nextConsumer: Option[RuleConsumer] = None

  final protected def nextConsumer: Option[RuleConsumer] = _nextConsumer

  def send(rule: Rule): Unit

  def result: ForEach[FinalRule]

  def withListener(listener: PartialFunction[Event, Unit]): RuleConsumer

  final def ~>(consumer: RuleConsumer): RuleConsumer = {
    _nextConsumer = Some(consumer)
    consumer
  }

  protected def listener: PartialFunction[Event, Unit]

  final protected def invokeEvent(event: Event): Unit = if (listener.isDefinedAt(event)) listener(event)

}

object RuleConsumer {

  trait Event

  trait NoEventRuleConsumer extends RuleConsumer {
    protected def listener: PartialFunction[Event, Unit] = PartialFunction.empty

    def withListener(listener: PartialFunction[Event, Unit]): RuleConsumer = this
  }

  trait Invoker[T] extends {
    def invoke(f: RuleConsumer => T)(implicit tripleIndex: TripleIndex[Int], mapper: TripleItemIndex): T

    def ~>(invoker: Invoker[T]): Invoker[T] = new Invokers(Vector(this, invoker))
  }

  private class Invokers[T](invokers: Vector[Invoker[T]]) extends Invoker[T] {
    def invoke(f: RuleConsumer => T)(implicit tripleIndex: TripleIndex[Int], mapper: TripleItemIndex): T = {
      def invokeRecursively(consumers: Vector[RuleConsumer], invokers: Seq[Invoker[T]]): T = {
        invokers match {
          case Seq(head, tail@_*) => head.invoke(consumer => invokeRecursively(consumers :+ consumer, tail))
          case _ =>
            consumers.reduce(_ ~> _)
            f(consumers.head)
        }
      }

      invokeRecursively(Vector.empty, invokers)
    }

    override def ~>(invoker: Invoker[T]): Invoker[T] = new Invokers(invokers :+ invoker)
  }

  /*final class Invoker[T] private[RuleConsumer](gs: Seq[InvokerType[T]]) {
    def invoke(f: RuleConsumer => T)(implicit tripleIndex: TripleIndex[Int], mapper: TripleItemIndex): T = {
      def invokeRecursively(consumers: Vector[RuleConsumer], invorkers: Seq[InvokerType[T]]): T = {
        invorkers match {
          case Seq(head, tail@_*) => head(tripleIndex)(mapper)(consumer => invokeRecursively(consumers :+ consumer, tail))
          case _ =>
            consumers.reduce(_ ~> _)
            f(consumers.head)
        }
      }
      invokeRecursively(Vector.empty, gs)
    }
  }*/

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

}