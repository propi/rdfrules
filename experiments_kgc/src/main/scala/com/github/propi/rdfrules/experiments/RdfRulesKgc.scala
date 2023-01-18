package com.github.propi.rdfrules.experiments

import com.github.propi.rdfrules.data.{Dataset, Graph}
import com.github.propi.rdfrules.experiments.benchmark.Benchmark._
import com.github.propi.rdfrules.experiments.benchmark.MetricResultProcessor.BasicPrinter
import com.github.propi.rdfrules.experiments.benchmark._
import com.github.propi.rdfrules.experiments.benchmark.metrics.RulesetMetric
import com.github.propi.rdfrules.experiments.benchmark.tasks._
import com.github.propi.rdfrules.prediction.aggregator.JointScorer
import com.github.propi.rdfrules.rule.{DefaultConfidence, Measure}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.{Debugger, HowLong}
import org.apache.commons.cli.{Options, PosixParser}

import java.io.PrintStream
import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 7. 5. 2019.
  */
object RdfRulesKgc {

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
    options.addOption("dataset", true, "input TSV dataset")
    options.addOption("output", true, "output file")

    options.addOption("runcomparativetest", false, "run comparative test")
    options.addOption("runconfidences", false, "run comparative test")

    options.addOption("rdfrulesonly", false, "run only rdf rules tests")
    options.addOption("anyburlonly", false, "run only anyburl tests")

    println(s"Number of cores: ${Runtime.getRuntime.availableProcessors()}")

    val cli = parser.parse(options, args)

    val numberOfThreads = cli.getOptionValue("cores", Runtime.getRuntime.availableProcessors().toString).toInt
    val datasetName = cli.getOptionValue("dataset", "wn18rr")
    val outputFile = cli.getOptionValue("output", "experiments/data/results.txt")
    implicit val outputWriter: PrintStream = new PrintStream(outputFile)
    try {
      Debugger() { implicit debugger =>
        val trainPath = s"experiments/data/$datasetName/train.tsv"
        val validPath = s"experiments/data/$datasetName/valid.tsv"
        val testPath = s"experiments/data/$datasetName/test.tsv"
        lazy val test = Dataset(testPath)
        lazy val trainValidPath = {
          (Graph(trainPath).toDataset + Graph(validPath)).`export`(s"experiments/data/$datasetName/trainValid.tsv")
          s"experiments/data/$datasetName/trainValid.tsv"
        }
        lazy val index = {
          val index = Graph(trainValidPath).index().withEvaluatedLazyVals
          HowLong.howLong("RDFRules indexing", memUsage = true, forceShow = true) {
            index.tripleMap.size(true)
          }
          index
        }
        /*if (cli.hasOption("runcomparativetest")) {

          for (minHc <- minHcs) {
            val taskDesc = s"minHeadCoverage = $minHc, minConfidence = 0.1, minPcaConfidence = 0.1, only logical rules"
            val taskResult1 = if (!cli.hasOption("rdfrulesonly")) Some(
              Once executeTask AnyBurlMiningTask withInput settings
              xTimes executeTask new MinHcAmie(s"AMIE: $taskDesc", minHc, numberOfThreads = numberOfThreads) withInput inputTsvDataset andAggregateResultWith StatsAggregator
            ) else None
            val taskResult2 = if (!cli.hasOption("anyburlonly")) Some(
              xTimes executeTask new MinHcRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", minHc, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor withInput index andAggregateResultWith StatsAggregator
            ) else None
            (taskResult1, taskResult2) match {
              case (Some(tr1), Some(tr2)) => tr1 compareWith tr2 andFinallyProcessResultWith BasicPrinter()
              case (Some(tr1), _) => tr1 andFinallyProcessResultWith BasicPrinter()
              case (_, Some(tr2)) => tr2 andFinallyProcessResultWith BasicPrinter()
              case _ =>
            }
          }
        }*/
        //List(false, true).foreach { anytime =>
        if (cli.hasOption("runconfidences")) {
            val ruleset = {
              implicit val rulesToMetrics: Ruleset => Seq[Metric] = rules => List(new RulesetMetric("ruleset", rules))
              val res = Once executeTask new RdfRulesKgcMiningTask(s"RDFRules mining", numberOfThreads, 4, false) withInput index
              res andFinallyProcessAndReturnResultWith BasicPrinter()
            }
            {
              implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.CwaConfidence)
              Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, CWA, Joint", test, JointScorer()) with PredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
            }
            {
              implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.PcaConfidence)
              Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, PCA, Joint", test, JointScorer()) with PredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
            }
            {
              implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.QpcaConfidence)
              Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, QPCA, Joint", test, JointScorer()) with PredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
            }
        }
        /**if (cli.hasOption("runlift")) {
          val ruleset = {
            implicit val rulesToMetrics: Ruleset => Seq[Metric] = rules => List(new RulesetMetric("ruleset", rules))
            val res = Once executeTask new RdfRulesKgcMiningTask(s"RDFRules mining", numberOfThreads, 4, false) withInput index
            res andFinallyProcessAndReturnResultWith BasicPrinter()
          }
          {
            implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.CwaConfidence)
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, CWA, Joint", test, JointScorer()) with PredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          }
          {
            implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.PcaConfidence)
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, PCA, Joint", test, JointScorer()) with PredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          }
          {
            implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.QpcaConfidence)
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, QPCA, Joint", test, JointScorer()) with PredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          }
        }*/
      }
    } finally {
      outputWriter.close()
    }
    HowLong.flushAllResults()
  }

}