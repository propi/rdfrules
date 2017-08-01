package eu.easyminer.rdf.task

import eu.easyminer.rdf.rule.ExtendedRule.ClosedRule

/**
  * Created by Vaclav Zeman on 12. 7. 2017.
  */
trait TaskResultWriter {

  def writeInputTask(inputTask: InputTaskParser.InputTask)

  def writeRules(rules: List[ClosedRule])

  def close()

}
