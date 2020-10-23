package com.github.propi.rdfrules.algorithm

import com.github.propi.rdfrules.algorithm.RuleConsumer.Result
import com.github.propi.rdfrules.rule.Rule

trait RuleConsumer {

  def send(rule: Rule): Unit

  def result: Result

}

object RuleConsumer {

  case class Result(rules: Traversable[Rule.Simple], isCached: Boolean)

  trait Builder {
    def build: RuleConsumer
  }

}