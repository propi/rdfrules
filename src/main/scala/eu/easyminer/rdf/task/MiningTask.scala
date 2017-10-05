package eu.easyminer.rdf.task

import eu.easyminer.rdf.algorithm.amie.Amie
import eu.easyminer.rdf.index.TripleHashIndex
import eu.easyminer.rdf.rule.{Measure, RuleConstraint, RulePattern, Threshold}
import eu.easyminer.rdf.utils.Debugger

/**
  * Created by Vaclav Zeman on 12. 7. 2017.
  */
trait MiningTask[T] {

  self: InputTaskParser[T] with TaskResultWriter =>

  protected val tripleHashIndex: TripleHashIndex
  protected implicit val debugger: Debugger

  private def buildAmie(inputTask: InputTaskParser.InputTask) = {
    val it: Iterator[Any] = inputTask.thresholds.m.valuesIterator ++ inputTask.constraints.iterator ++ Iterator(inputTask.rulePattern)
    it.foldLeft(Amie()) { (amie, x) =>
      x match {
        case x: Threshold => amie.addThreshold(x)
        case x: RuleConstraint => amie.addConstraint(x)
        case Some(x: RulePattern) => amie.setRulePattern(x)
        case _ => amie
      }
    }
  }

  def runTask(inputTask: T) = try {
    val input = self.parse(inputTask)
    writeInputTask(input)
    val rules = buildAmie(input)
      .mine(tripleHashIndex)
      .sortBy(x => (x.measures(Measure.Confidence).asInstanceOf[Measure.Confidence].value, x.measures(Measure.HeadCoverage).asInstanceOf[Measure.HeadCoverage].value))(Ordering[(Double, Double)].reverse)
    writeRules(rules)
  } finally {
    close()
  }

}