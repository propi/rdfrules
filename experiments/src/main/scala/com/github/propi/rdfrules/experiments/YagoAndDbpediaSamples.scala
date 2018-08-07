package com.github.propi.rdfrules.experiments

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.algorithm.dbscan.DbScan
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.ruleset._
import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 24. 4. 2018.
  */
object YagoAndDbpediaSamples {

  private def yagoLogicalRulesExample1(implicit debugger: Debugger) = new Example[Ruleset] {
    def name: String = "Logical rules mining from a YAGO sample with default params"

    protected def example: Ruleset = {
      Dataset(Example.experimentsDir + "yago.tsv")
        .mine(Amie())
        .sorted
        .take(10)
    }
  }

  private def yagoLogicalRulesTopKExample2(implicit debugger: Debugger) = new Example[Ruleset] {
    def name: String = "Logical rules mining from a YAGO sample with the top-k approach."

    protected def example: Ruleset = {
      Dataset(Example.experimentsDir + "yago.tsv")
        .mine(Amie().addThreshold(Threshold.TopK(10)))
        .sorted
    }
  }

  private def yagoLogicalRulesWithManyParamsExample3(implicit debugger: Debugger) = new Example[Ruleset] {
    def name: String = "Logical rules mining from a YAGO sample with many mining params."

    protected def example: Ruleset = {
      val dataset = Dataset(Example.experimentsDir + "yago.tsv")
      dataset.mine(Amie()
        .addThreshold(Threshold.MinHeadSize(80))
        .addThreshold(Threshold.MinHeadCoverage(0.001))
        .addThreshold(Threshold.TopK(1000))
        .addConstraint(RuleConstraint.WithInstances(true)))
        .computePcaConfidence(0.5)
        .computeLift()
        .makeClusters(DbScan(minNeighbours = 2))
        .sortBy(Measure.Cluster, Measure.PcaConfidence, Measure.Lift, Measure.HeadCoverage)
        .cache(Example.resultDir + "rules-example3.cache")
      val ruleset = Ruleset.fromCache(dataset.index(), Example.resultDir + "rules-example3.cache")
      ruleset.export(Example.resultDir + "rules-example3.txt")
      ruleset.export(Example.resultDir + "rules-example3.json")
      ruleset
    }
  }

  private def yagoWithoutDbpediaExample4(implicit debugger: Debugger) = new Example[Ruleset] {
    def name: String = "Logical rules mining only from a YAGO graph."

    protected def example: Ruleset = {
      val dataset = Graph("yago", Example.experimentsDir + "yagoLiteralFacts.tsv").toDataset +
        Graph("yago", Example.experimentsDir + "yagoFacts.tsv") +
        Graph("yago", Example.experimentsDir + "yagoDBpediaInstances.tsv")
      dataset.mine(Amie()
        .addThreshold(Threshold.MinHeadCoverage(0.2)))
        .sorted
    }
  }

  private def yagoAndDbpediaMultigraphsMiningExample5(implicit debugger: Debugger) = new Example[Ruleset] {
    def name: String = "Logical rules mining across two linked graphs: YAGO and DBpedia"

    protected def example: Ruleset = {
      val dataset = Graph("yago", Example.experimentsDir + "yagoLiteralFacts.tsv").toDataset +
        Graph("yago", Example.experimentsDir + "yagoFacts.tsv") +
        Graph("yago", Example.experimentsDir + "yagoDBpediaInstances.tsv") +
        Graph("dbpedia", Example.experimentsDir + "mappingbased_objects_sample.ttl") +
        Graph("dbpedia", Example.experimentsDir + "mappingbased_literals_sample.ttl")
      dataset.mine(Amie()
        .addThreshold(Threshold.MinHeadCoverage(0.2))
        .addPattern(AtomPattern(graph = TripleItem.Uri("dbpedia")) =>: AtomPattern(graph = TripleItem.Uri("yago")))
        .addPattern(AtomPattern(graph = TripleItem.Uri("yago")) =>: AtomPattern(graph = TripleItem.Uri("dbpedia"))))
        .sorted
        .graphBasedRules
        .cache(Example.resultDir + "rules-example5.cache")
      Ruleset.fromCache(dataset.index(), Example.resultDir + "rules-example5.cache")
    }
  }

  def main(args: Array[String]): Unit = {
    Example.prepareResultsDir()
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