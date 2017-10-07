package eu.easyminer.rdf.task

import eu.easyminer.rdf.rule.{RuleConstraint, RulePattern, Threshold}
import eu.easyminer.rdf.task.InputTaskParser.InputTask

/**
  * Created by Vaclav Zeman on 12. 7. 2017.
  */
trait InputTaskParser[T] {

  def parse(inputTask: T): InputTask

}

object InputTaskParser {

  class InputTask(val name: String, val thresholds: Threshold.Thresholds, val rulePattern: Option[RulePattern], val constraints: List[RuleConstraint])

}