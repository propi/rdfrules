package com.github.propi.rdfrules.experiments

import com.github.propi.rdfrules.data.{Dataset, Graph}
import com.github.propi.rdfrules.experiments.benchmark.Benchmark._
import com.github.propi.rdfrules.experiments.benchmark.MetricResultProcessor.BasicPrinter
import com.github.propi.rdfrules.experiments.benchmark._
import com.github.propi.rdfrules.experiments.benchmark.metrics.RulesetMetric
import com.github.propi.rdfrules.prediction.aggregator.{MaximumScorer, NoisyOrScorer, NonRedundantTopRules, TopRules}
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition
import com.github.propi.rdfrules.rule.{DefaultConfidence, Measure}
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.{Debugger, Functor, HowLong}
import org.apache.commons.cli.{Options, PosixParser}

import java.io.{File, PrintStream}
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
    options.addOption("rlen", true, "rule length")
    options.addOption("revalidate", false, "revalidate caches")

    options.addOption("runanyburl", false, "run anyburl algorithm for rules mining")
    options.addOption("runconfidences", false, "run prediction with different confidence types")
    options.addOption("runmodes", false, "run prediction with or without zero rules")
    options.addOption("runconstants", false, "run mining with different constant types and then prediction")
    options.addOption("runanytime", false, "run mining with anytime approach and then prediction")
    options.addOption("runscorers", false, "run prediction with different scorers")

    //options.addOption("runlen2", false, "")
    options.addOption("runpruning", false, "")
    //options.addOption("runallheadconstants", false, "")
    //options.addOption("runnoheadcoverage", false, "")

    println(s"Number of cores: ${Runtime.getRuntime.availableProcessors()}")

    val cli = parser.parse(options, args)

    val numberOfThreads = cli.getOptionValue("cores", Runtime.getRuntime.availableProcessors().toString).toInt
    val datasetName = cli.getOptionValue("dataset", "wn18rr")
    val outputFile = cli.getOptionValue("output", "experiments/data/results.txt")
    val outputFolder = outputFile.replaceFirst("/[^/]+$", "")
    val ruleLength = cli.getOptionValue("rlen", "3").toInt
    val revalidate = cli.hasOption("revalidate")
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
          val index = Graph(trainValidPath).index
          HowLong.howLong("RDFRules indexing", memUsage = true, forceShow = true) {
            index.main.tripleMap.evaluateAllLazyVals()
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

        def mineRules(id: String, anytime: Boolean, constants: Option[ConstantsAtPosition.ConstantsPosition], noHeadCoverage: Boolean) = {
          val rulesCache = new File(s"$outputFolder/${if (id.isEmpty) "" else s"$id-"}$datasetName-rules.ndjson")
          val taskName = s"RDFRules mining: $id"
          if (revalidate || !rulesCache.isFile) {
            implicit val rulesToMetrics: Ruleset => Seq[Metric] = rules => List(new RulesetMetric("ruleset", rules))
            val res = Once executeTask new RdfRulesKgcMiningTask(taskName, rulesCache.getAbsolutePath, numberOfThreads, ruleLength, anytime, constants, noHeadCoverage) withInput index
            res andFinallyProcessAndReturnResultWith BasicPrinter()
          } else {
            val res = Ruleset(index, rulesCache).setParallelism(numberOfThreads).withDebugger().cache
            (taskName, res, List(new RulesetMetric("ruleset", res))) andFinallyProcessAndReturnResultWith BasicPrinter()
          }
        }

        def computeConfidence(id: String, ruleset: Ruleset)(implicit defaultConfidence: DefaultConfidence) = {
          val confName = defaultConfidence.confidenceType.get match {
            case Measure.PcaConfidence => "pca"
            case Measure.QpcaConfidence => "qpca"
            case Measure.CwaConfidence => "cwa"
          }
          val rulesCache = new File(s"$outputFolder/${if (id.isEmpty) "" else s"$id-"}$datasetName-$confName-rules.ndjson")
          val taskName = s"Confidence computing $confName: $id"
          if (revalidate || !rulesCache.isFile) {
            implicit val rulesToMetrics: Ruleset => Seq[Metric] = rules => List(new RulesetMetric("ruleset", rules))
            Once executeTask new ConfidenceComputingTask(taskName, rulesCache.getAbsolutePath) withInput ruleset andFinallyProcessAndReturnResultWith BasicPrinter()
          } else {
            val res = Ruleset(index, rulesCache).setParallelism(numberOfThreads).withDebugger().cache
            (taskName, res, List(new RulesetMetric("ruleset", res))) andFinallyProcessAndReturnResultWith BasicPrinter()
          }
        }

        def clustering(id: String, ruleset: Ruleset) = {
          val rulesCache = new File(s"$outputFolder/${if (id.isEmpty) "" else s"$id-"}$datasetName-clustered-rules.ndjson")
          val taskName = s"Clustering task: $id"
          if (revalidate || !rulesCache.isFile) {
            implicit val rulesToMetrics: Ruleset => Seq[Metric] = rules => List(new RulesetMetric("ruleset", rules))
            Once executeTask new ClusteringTask(taskName, rulesCache.getAbsolutePath) withInput ruleset andFinallyProcessAndReturnResultWith BasicPrinter()
          } else {
            val res = Ruleset(index, rulesCache).setParallelism(numberOfThreads).withDebugger().cache
            (taskName, res, List(new RulesetMetric("ruleset", res))) andFinallyProcessAndReturnResultWith BasicPrinter()
          }
        }

        lazy val exactRuleset = mineRules("", false, Some(ConstantsAtPosition.ConstantsPosition.LowerCardinalitySide()), true)
        lazy val exactConfRuleset = computeConfidence("", exactRuleset)(DefaultConfidence(Measure.QpcaConfidence))
        lazy val exactClusteredRuleset = clustering("", exactConfRuleset)

        if (cli.hasOption("runconfidences")) {
          val ruleset = exactRuleset
          Functor(DefaultConfidence(Measure.CwaConfidence)).foreach { implicit defaultConfidence =>
            val confRuleset = computeConfidence("", ruleset)
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, CWA", test, MaximumScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput confRuleset andFinallyProcessResultWith BasicPrinter()
          }
          Functor(DefaultConfidence(Measure.PcaConfidence)).foreach { implicit defaultConfidence =>
            val confRuleset = computeConfidence("", ruleset)
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, PCA", test, MaximumScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput confRuleset andFinallyProcessResultWith BasicPrinter()
          }
          Functor(DefaultConfidence(Measure.QpcaConfidence)).foreach { implicit defaultConfidence =>
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, QPCA", test, MaximumScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput exactConfRuleset andFinallyProcessResultWith BasicPrinter()
          }
        }
        if (cli.hasOption("runmodes")) {
          implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.QpcaConfidence)
          Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction without modes", test, MaximumScorer(), TopRules(100)) with PredictionTaskPostprocessor withInput exactConfRuleset andFinallyProcessResultWith BasicPrinter()
        }
        if (cli.hasOption("runconstants")) {
          implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.QpcaConfidence)
          Functor(mineRules("noconstants", false, Some(ConstantsAtPosition.ConstantsPosition.Nowhere), true)).map { ruleset =>
            computeConfidence("noconstants", ruleset)
          }.foreach { ruleset =>
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, NoConstants", test, MaximumScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          }
          /*Functor(mineRules("constantslower", false, Some(ConstantsAtPosition.ConstantsPosition.LowerCardinalitySide()), true)).map { ruleset =>
            computeConfidence("constantslower", ruleset)
          }.foreach { ruleset =>
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, ConstantsLowerCadinalitySide", test, MaximumScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          }*/
        }
        if (cli.hasOption("runanytime")) {
          implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.QpcaConfidence)
          Functor(mineRules("anytime", true, None, true)).map { ruleset =>
            computeConfidence("anytime", ruleset)
          }.foreach { ruleset =>
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, Anytime", test, MaximumScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          }
        }
        if (cli.hasOption("runscorers")) {
          implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.QpcaConfidence)
          Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, NoisyOr", test, NoisyOrScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput exactConfRuleset andFinallyProcessResultWith BasicPrinter()
          Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, NonRedundantNoisyOr", test, NoisyOrScorer(), NonRedundantTopRules(100)) with ModesPredictionTaskPostprocessor withInput exactClusteredRuleset andFinallyProcessResultWith BasicPrinter()
          Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, NonRedundantMaximum", test, MaximumScorer(), NonRedundantTopRules(100)) with ModesPredictionTaskPostprocessor withInput exactClusteredRuleset andFinallyProcessResultWith BasicPrinter()
        }
        /*if (cli.hasOption("runlen2")) {
          implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.QpcaConfidence)
          val ruleset = computeConfidence("len2", mineRules("len2"))
          Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, RuleLength = 2", test, MaximumScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput ruleset.filter(_.ruleLength == 2) andFinallyProcessResultWith BasicPrinter()
        }*/
        if (cli.hasOption("runpruning")) {
          implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.QpcaConfidence)
          implicit val rulesToMetrics: Ruleset => Seq[Metric] = rules => List(new RulesetMetric("ruleset", rules))
          val ruleset = Once executeTask new Task[Ruleset, Ruleset, Ruleset, Ruleset] with TaskPreProcessor[Ruleset, Ruleset] with TaskPostProcessor[Ruleset, Ruleset] {
            val name: String = "Pruning"

            protected def taskBody(input: Ruleset): Ruleset = {
              val ruleset = exactConfRuleset.sorted.pruned(onlyFunctionalProperties = false).cache
              ruleset.size
              ruleset
            }

            protected def preProcess(input: Ruleset): Ruleset = input

            protected def postProcess(result: Ruleset): Ruleset = result
          } withInput exactConfRuleset andFinallyProcessAndReturnResultWith BasicPrinter()
          Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, pruning", test, MaximumScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
        }
        if (cli.hasOption("runanyburl")) {
          implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.CwaConfidence)
          val anyBurlSettings = AnyBurlSettings(trainValidPath, validPath, testPath, outputFolder, 1000, 5, numberOfThreads, ruleLength, false)
          val ruleset = Ruleset(
            index,
            Once executeTask AnyBurlMiningTask withInput anyBurlSettings andFinallyProcessAndReturnResultWith BasicPrinter()
          ).withDebugger().cache
          println(ruleset.size)
          Once executeTask new PredictionTask[Seq[Metric]]("AnyBURL prediction task, all", test, MaximumScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          Once executeTask new PredictionTask[Seq[Metric]]("AnyBURL prediction task, closed", test, MaximumScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput ruleset.filter(_.isClosed) andFinallyProcessResultWith BasicPrinter()
        }
      }
    } finally {
      outputWriter.close()
    }
    HowLong.flushAllResults()
  }

}