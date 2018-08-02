package com.github.propi.rdfrules.experiments

import java.io._

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.algorithm.dbscan.DbScan
import com.github.propi.rdfrules.data.TripleItem.Uri
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.ruleset.{Ruleset, RulesetSource}
import com.github.propi.rdfrules.utils.Debugger
import org.apache.commons.io.FileUtils
import com.github.propi.rdfrules.ruleset.formats.Text._
import com.github.propi.rdfrules.ruleset.formats.Json._
import com.github.propi.rdfrules.stringifier.CommonStringifiers._

/**
  * Created by Vaclav Zeman on 24. 4. 2018.
  */
object YagoAndDbpediaSamples {

  private def yagoLogicalRulesExample1(implicit debugger: Debugger) = new Example[Ruleset] {
    def name: String = "Logical rules mining from a YAGO sample with default params"

    protected def example: Ruleset = {
      Dataset(new File("experiments/data/yago.tsv"))
        .mine(Amie())
        .sorted
        .take(10)
    }
  }

  private def yagoLogicalRulesTopKExample2(implicit debugger: Debugger) = new Example[Ruleset] {
    def name: String = "Logical rules mining from a YAGO sample with the top-k approach."

    protected def example: Ruleset = {
      Dataset(new File("experiments/data/yago.tsv"))
        .mine(Amie().addThreshold(Threshold.TopK(10)))
        .sorted
    }
  }

  private def yagoLogicalRulesWithManyParamsExample3(implicit debugger: Debugger) = new Example[Ruleset] {
    def name: String = "Logical rules mining from a YAGO sample with many mining params."

    protected def example: Ruleset = {
      val dataset = Dataset(new File("experiments/data/yago.tsv"))
      dataset.mine(Amie()
        .addThreshold(Threshold.MinHeadSize(80))
        .addThreshold(Threshold.MinHeadCoverage(0.001))
        .addThreshold(Threshold.TopK(1000))
        .addConstraint(RuleConstraint.WithInstances(true)))
        .countPcaConfidence(0.5)
        .countLift()
        .countClusters(DbScan(minNeighbours = 2))
        .sortBy(Measure.Cluster, Measure.PcaConfidence, Measure.Lift, Measure.HeadCoverage)
        .cache(new File("experiments/results/rules-example3.cache"))
      val ruleset = Ruleset.fromCache(dataset.index(), new File("experiments/results/rules-example3.cache"))
      ruleset.export[RulesetSource.Text.type](new File("experiments/results/rules-example3.txt"))
      ruleset.export[RulesetSource.Json.type](new File("experiments/results/rules-example3.json"))
      ruleset
    }
  }

  private def yagoWithoutDbpediaExample4(implicit debugger: Debugger) = new Example[Ruleset] {
    def name: String = "Logical rules mining only from a YAGO graph."

    protected def example: Ruleset = {
      val dataset = Graph("yago", new File("experiments/data/yagoLiteralFacts.tsv")).toDataset +
        Graph("yago", new File("experiments/data/yagoFacts.tsv")) +
        Graph("yago", new File("experiments/data/yagoDBpediaInstances.tsv"))
      dataset.mine(Amie()
        .addThreshold(Threshold.MinHeadCoverage(0.2)))
        .sorted
    }
  }

  private def yagoAndDbpediaMultigraphsMiningExample5(implicit debugger: Debugger) = new Example[Ruleset] {
    def name: String = "Logical rules mining across two linked graphs: YAGO and DBpedia"

    protected def example: Ruleset = {
      val dataset = Graph("yago", new File("experiments/data/yagoLiteralFacts.tsv")).toDataset +
        Graph("yago", new File("experiments/data/yagoFacts.tsv")) +
        Graph("yago", new File("experiments/data/yagoDBpediaInstances.tsv")) +
        Graph("dbpedia", new File("experiments/data/mappingbased_objects_sample.ttl")) +
        Graph("dbpedia", new File("experiments/data/mappingbased_literals_sample.ttl"))
      dataset.index().useMapper { implicit mapper =>
        _.mine(Amie()
          .addThreshold(Threshold.MinHeadCoverage(0.2))
          .addPattern(AtomPattern(graph = Uri("dbpedia")) =>: AtomPattern(graph = Uri("yago")))
          .addPattern(AtomPattern(graph = Uri("yago")) =>: AtomPattern(graph = Uri("dbpedia"))))
          .sorted
          .graphBasedRules
          .cache(new File("experiments/results/rules-example5.cache"))
      }
      Ruleset.fromCache(dataset.index(), new File("experiments/results/rules-example5.cache"))
    }
  }

  def main(args: Array[String]): Unit = {
    val resultsDir = new File("experiments/results")
    if (!resultsDir.isDirectory) resultsDir.mkdirs()
    FileUtils.cleanDirectory(resultsDir)
    Debugger() { implicit debugger =>
      val examples = List(
        yagoLogicalRulesExample1,
        yagoLogicalRulesTopKExample2,
        yagoLogicalRulesWithManyParamsExample3,
        yagoWithoutDbpediaExample4,
        yagoAndDbpediaMultigraphsMiningExample5
      )
      for (example <- examples) {
        example.execute
      }
    }
  }

}