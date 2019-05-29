package com.github.propi.rdfrules.experiments

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.ruleset._
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 24. 4. 2018.
  */
object CompleteWorkflowScala {

  private def scalaApiExample1(implicit debugger: Debugger) = new Example[Ruleset] {
    def name: String = "Complete Workflow with the Scala API"

    protected def example: Ruleset = {
      val ruleset = (Dataset() +
        Graph("yago", Example.experimentsDir + "yagoLiteralFacts.tsv.bz2") +
        Graph("yago", Example.experimentsDir + "yagoFacts.tsv.bz2"))
        .filter(!_.triple.predicate.hasSameUriAs("participatedIn"))
        .discretize(DiscretizationTask.Equifrequency(3))(_.triple.predicate.hasSameUriAs("hasNumberOfPeople"))
        .mine(Amie()
          .addConstraint(RuleConstraint.WithInstances(true))
          .addPattern(AtomPattern(predicate = TripleItem.Uri("hasNumberOfPeople")) =>: None)
          .addPattern(AtomPattern(predicate = TripleItem.Uri("hasNumberOfPeople")))
        )
        .computePcaConfidence(0.5)
        .sorted
        .cache
      ruleset.export(Example.resultDir + "rules-workflow-scala.json")
      ruleset.export(Example.resultDir + "rules-workflow-scala.txt")
      ruleset
    }
  }

  def main(args: Array[String]): Unit = {
    Example.prepareResultsDir()
    Debugger() { implicit debugger =>
      scalaApiExample1.execute
    }
  }

}