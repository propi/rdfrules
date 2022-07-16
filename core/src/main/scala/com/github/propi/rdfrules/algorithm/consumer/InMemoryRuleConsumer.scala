package com.github.propi.rdfrules.algorithm.consumer

import com.github.propi.rdfrules.algorithm.RuleConsumer
import com.github.propi.rdfrules.rule.Rule
import com.github.propi.rdfrules.rule.Rule.FinalRule
import com.github.propi.rdfrules.utils.ForEach

import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import scala.jdk.CollectionConverters._
import scala.language.postfixOps

class InMemoryRuleConsumer private(prettyPrintedFile: Option[(File, File => RuleWriter)]) extends RuleConsumer.NoEventRuleConsumer {

  private val rules = new ConcurrentLinkedQueue[FinalRule]

  def send(rule: Rule): Unit = {
    val ruleSimple = Rule(rule)
    rules.add(ruleSimple)
    nextConsumer.foreach(_.send(ruleSimple))
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

  def apply[T](f: InMemoryRuleConsumer => T): T = f(new InMemoryRuleConsumer(None))

}