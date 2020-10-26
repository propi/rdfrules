package com.github.propi.rdfrules.algorithm

import com.github.propi.rdfrules.algorithm.RuleConsumer.{Event, Result}
import com.github.propi.rdfrules.rule.Rule

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

  case class Result(rules: Traversable[Rule.Simple], isCached: Boolean, size: Int)

}