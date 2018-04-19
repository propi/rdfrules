package com.github.propi.rdfrules.ruleset

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
trait RulesetSource

object RulesetSource {

  case object Text extends RulesetSource

  case object Json extends RulesetSource

  implicit def rulesetSourceToRulesetWriter[T <: RulesetSource](rulesetSource: T)(implicit rulesetWriter: RulesetWriter[T]): RulesetWriter[T] = rulesetWriter

}
