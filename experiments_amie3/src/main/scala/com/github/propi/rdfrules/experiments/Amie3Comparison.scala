package com.github.propi.rdfrules.experiments

import com.github.propi.rdfrules.data.Graph
import com.github.propi.rdfrules.experiments.InputData.getInputTsvDataset
import com.github.propi.rdfrules.experiments.benchmark.Benchmark._
import com.github.propi.rdfrules.experiments.benchmark.MetricResultProcessor.BasicPrinter
import com.github.propi.rdfrules.experiments.benchmark.MetricsAggregator.StatsAggregator
import com.github.propi.rdfrules.experiments.benchmark._
import com.github.propi.rdfrules.experiments.benchmark.tasks._
import com.github.propi.rdfrules.rule.ResolvedRule
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.utils.{Debugger, HowLong}
import org.apache.commons.cli.{Options, PosixParser}

import java.io.PrintStream
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 7. 5. 2019.
  */
object Amie3Comparison {

  /**
    * For restriction of threads number use these jvm arguments:
    * -Dscala.concurrent.context.minThreads=8 -Dscala.concurrent.context.numThreads=8 -Dscala.concurrent.context.maxThreads=8
    *
    * @param args args
    */
  def main(args: Array[String]): Unit = {
    val parser = new PosixParser
    val options = new Options
    options.addOption("cores", true, "max number of cores")
    options.addOption("minhcs", true, "list of min head coverages")
    options.addOption("input", true, "input TSV dataset")
    options.addOption("output", true, "output file")
    options.addOption("times", true, "number of repetition of each task")

    options.addOption("runconstants", false, "run mining with constants test")
    options.addOption("runlogical", false, "run mining only logical rules test")

    options.addOption("rdfrulesonly", false, "run only rdf rules tests")
    options.addOption("amieonly", false, "run only amie+ tests")

    println(s"Number of cores: ${Runtime.getRuntime.availableProcessors()}")

    val cli = parser.parse(options, args)

    val numberOfThreads = cli.getOptionValue("cores", Runtime.getRuntime.availableProcessors().toString).toInt
    val minHcs = cli.getOptionValue("minhcs", "0.005,0.01,0.02,0.05,0.1,0.2,0.3").split(",").iterator.map(_.trim).filter(_.nonEmpty).map(_.toDouble).toList
    val inputTsvDataset = getInputTsvDataset(cli.getOptionValue("input", "experiments/data/yago2core_facts.clean.notypes.tsv.bz2"))
    //experiments/data/mappingbased_objects_sample.tsv.bz2
    val outputFile = cli.getOptionValue("output", "experiments/data/results.txt")
    val xTimes = cli.getOptionValue("times", "7").toInt times
    implicit val outputWriter: PrintStream = new PrintStream(outputFile)
    try {
      Debugger() { implicit debugger =>
        lazy val index = {
          val index = Graph(inputTsvDataset).index().withEvaluatedLazyVals
          HowLong.howLong("RDFRules indexing", memUsage = true, forceShow = true) {
            index.tripleMap.size(true)
          }
          index
        }
        if (cli.hasOption("runlogical")) {
          for (minHc <- minHcs) {
            val taskDesc = s"minHeadCoverage = $minHc, minPcaConfidence = 0.5, only logical rules"
            val taskResult1 = if (!cli.hasOption("rdfrulesonly")) Some(
              xTimes executeTask new MinHcAmie(s"AMIE: $taskDesc", minHc, numberOfThreads = numberOfThreads) {
                override val minPcaConfidence: Double = 0.5
                override val minConfidence: Double = 0.0
                override val injectiveMapping: Boolean = false
                override val amie3Optimization: Boolean = true
              } withInput inputTsvDataset andAggregateResultWith StatsAggregator
            ) else None
            val taskResult2 = if (!cli.hasOption("amieonly")) Some(
              xTimes executeTask new MinHcRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", minHc, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor {
                override val minPcaConfidence: Double = 0.5
                override val minConfidence: Double = 0.0
                override val skylinePruning: Boolean = true
              } withInput index andAggregateResultWith StatsAggregator
            ) else None
            (taskResult1, taskResult2) match {
              case (Some(tr1), Some(tr2)) => tr1 compareWith tr2 andFinallyProcessResultWith BasicPrinter()
              case (Some(tr1), _) => tr1 andFinallyProcessResultWith BasicPrinter()
              case (_, Some(tr2)) => tr2 andFinallyProcessResultWith BasicPrinter()
              case _ =>
            }
          }
        }
        if (cli.hasOption("runconstants")) {
          for (minHc <- minHcs) {
            val taskDesc = s"minHeadCoverage = $minHc, minPcaConfidence = 0.5, with constants"
            val taskResult1 = if (!cli.hasOption("rdfrulesonly")) Some(
              xTimes executeTask new MinHcAmie(s"AMIE: $taskDesc", minHc, None, numberOfThreads = numberOfThreads) {
                override val minPcaConfidence: Double = 0.5
                override val minConfidence: Double = 0.0
                override val injectiveMapping: Boolean = false
                override val amie3Optimization: Boolean = true
              } withInput inputTsvDataset andAggregateResultWith StatsAggregator
            ) else None
            val taskResult2 = if (!cli.hasOption("amieonly")) Some(
              xTimes executeTask new MinHcRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", minHc, Some(ConstantsPosition.LowerCardinalitySide(true)), numberOfThreads = numberOfThreads) with RulesTaskPostprocessor {
                override val minPcaConfidence: Double = 0.5
                override val minConfidence: Double = 0.0
                override val skylinePruning: Boolean = true
              } withInput index andAggregateResultWith StatsAggregator
            ) else None
            (taskResult1, taskResult2) match {
              case (Some(tr1), Some(tr2)) => tr1 compareWith tr2 andFinallyProcessResultWith BasicPrinter()
              case (Some(tr1), _) => tr1 andFinallyProcessResultWith BasicPrinter()
              case (_, Some(tr2)) => tr2 andFinallyProcessResultWith BasicPrinter()
              case _ =>
            }
          }
        }
      }
    } finally {
      outputWriter.close()
    }
    HowLong.flushAllResults()
  }

}