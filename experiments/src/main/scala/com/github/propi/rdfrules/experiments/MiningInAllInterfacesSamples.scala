package com.github.propi.rdfrules.experiments

import java.io._

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting.{AtomsSimilarityCounting, SupportSimilarityCounting}
import com.github.propi.rdfrules.algorithm.dbscan.{DbScan, SimilarityCounting}
import com.github.propi.rdfrules.data.TripleItem.Uri
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.index.Index.Mode
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.ruleset.formats.Text._
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource}
import com.github.propi.rdfrules.stringifier.CommonStringifiers._
import com.github.propi.rdfrules.utils.Debugger
import eu.easyminer.discretization.DiscretizationTask
import eu.easyminer.discretization.task.EquifrequencyDiscretizationTask
import org.apache.commons.io.FileUtils
import org.apache.jena.riot.{Lang, RDFFormat}

/**
  * Created by Vaclav Zeman on 24. 4. 2018.
  */
object MiningInAllInterfacesSamples {

  private def scalaApiExample1(implicit debugger: Debugger) = new Example[Ruleset] {
    def name: String = "Logical rules mining from a YAGO sample with default params"

    protected def example: Ruleset = {
      (Dataset() +
        Graph("yago", new File("experiments/data/yagoLiteralFacts.tsv")) +
        Graph("yago", new File("experiments/data/yagoFacts.tsv")))
        .filter(_.triple.predicate != TripleItem.Uri("participatedIn"))
        .discretize(new EquifrequencyDiscretizationTask {
          def getNumberOfBins: Int = 3

          def getBufferSize: Int = 1000000
        })(_.triple.predicate.hasSameUriAs("hasNumberOfPeople"))
        .mine(Amie()
          .addConstraint(RuleConstraint.WithInstances(true)))
        .countPcaConfidence(0.5)
        .sorted
      //.resolvedRules
      //.filter(x => (x.body :+ x.head).exists(_.predicate.hasSameUriAs("hasNumberOfPeople")))
    }
  }

  def main(args: Array[String]): Unit = {
    Debugger() { implicit debugger =>
      (Dataset() +
        Graph("yago", new File("experiments/data/yagoLiteralFacts.tsv")) +
        Graph("yago", new File("experiments/data/yagoFacts.tsv")))
        .filter(_.triple.predicate != TripleItem.Uri("participatedIn"))
        .discretize(new EquifrequencyDiscretizationTask {
          def getNumberOfBins: Int = 3

          def getBufferSize: Int = 1000000
        })(_.triple.predicate.hasSameUriAs("hasNumberOfPeople"))
        .mine(Amie()
          /*.addThreshold(Threshold.MinHeadSize(20))
          .addThreshold(Threshold.MinHeadCoverage(0.001))*/
          .addConstraint(RuleConstraint.WithInstances(true)))
        .countPcaConfidence(0.5)
        .sorted
        .resolvedRules
        .filter(x => (x.body :+ x.head).exists(_.predicate.hasSameUriAs("hasNumberOfPeople")))
        .foreach(println)
    }
  }

}