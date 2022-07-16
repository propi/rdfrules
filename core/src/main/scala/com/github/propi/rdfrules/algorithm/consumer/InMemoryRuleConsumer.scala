package com.github.propi.rdfrules.algorithm.consumer

import com.github.propi.rdfrules.algorithm.RuleConsumer
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.utils.ForEach

import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters._
import scala.language.postfixOps

class InMemoryRuleConsumer private extends RuleConsumer.NoEventRuleConsumer {

  private val rules = new ConcurrentLinkedQueue[FinalRule]

  def send(rule: FinalRule): Unit = {
    rules.add(rule)
    nextConsumer.foreach(_.send(rule))
  }

  def result: ForEach[FinalRule] = {
    nextConsumer.foreach(_.result)
    new ForEach[FinalRule] {
      override lazy val knownSize: Int = rules.size()

      def foreach(f: FinalRule => Unit): Unit = rules.iterator().asScala.foreach(f)
    }
  }

}

object InMemoryRuleConsumer {

  def apply[T]()(f: InMemoryRuleConsumer => T): T = f(new InMemoryRuleConsumer)

}