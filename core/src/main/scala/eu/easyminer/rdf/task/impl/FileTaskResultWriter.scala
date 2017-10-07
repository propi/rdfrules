package eu.easyminer.rdf.task.impl

import java.io.PrintWriter
import java.util.Date

import eu.easyminer.rdf.rule.ExtendedRule.ClosedRule
import eu.easyminer.rdf.task.InputTaskParser.InputTask
import eu.easyminer.rdf.task.TaskResultWriter

/**
  * Created by Vaclav Zeman on 12. 7. 2017.
  */
trait FileTaskResultWriter extends TaskResultWriter {

  /*self: RuleStringifier =>

  protected val writer: PrintWriter

  def writeInputTask(inputTask: InputTask): Unit = {
    writer.println("# Mining task: " + (new Date).toString)
    writer.println("# Task input: " + inputTask.name)
    writer.println("# Thresholds: " + inputTask.thresholds.m.valuesIterator.mkString(", "))
    writer.println("# Constraints: " + inputTask.constraints.mkString(", "))
    inputTask.rulePattern.foreach(rp => writer.println("# Rule pattern: " + rp))
  }

  def writeRules(rules: List[ClosedRule]): Unit = {
    writer.println("# RESULT")
    writer.println("# Number of rules: " + rules.length)
    writer.println("# RULES")
    for (rule <- rules) {
      writer.println(stringifyRule(rule))
    }
  }

  def close(): Unit = writer.close()
*/
}
