package com.github.propi.rdfrules.experiments

import com.github.propi.rdfrules.data.{Dataset, Graph}
import com.github.propi.rdfrules.experiments.benchmark.Benchmark._
import com.github.propi.rdfrules.experiments.benchmark.MetricResultProcessor.BasicPrinter
import com.github.propi.rdfrules.experiments.benchmark._
import com.github.propi.rdfrules.experiments.benchmark.metrics.RulesetMetric
import com.github.propi.rdfrules.prediction.PredictionTasksResults
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

    options.addOption("runcomparativetest", false, "run comparative test")
    options.addOption("runconfidences", false, "run prediction with different confidence types")
    options.addOption("runmodes", false, "run prediction with or without zero rules")
    options.addOption("runconstants", false, "run mining with different constant types and then prediction")
    options.addOption("runanytime", false, "run mining with anytime approach and then prediction")
    options.addOption("runscorers", false, "run prediction with different scorers")

    options.addOption("rdfrulesonly", false, "run only rdf rules tests")
    options.addOption("anyburlonly", false, "run only anyburl tests")

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

        def mineRules(id: String, anytime: Boolean = false, constants: ConstantsAtPosition.ConstantsPosition = ConstantsAtPosition.ConstantsPosition.LowerCardinalitySide(true)) = {
          val rulesCache = new File(s"$outputFolder/${if (id.isEmpty) "" else s"$id-"}$datasetName-rules.ndjson")
          val taskName = s"RDFRules mining: $id"
          if (revalidate || !rulesCache.isFile) {
            implicit val rulesToMetrics: Ruleset => Seq[Metric] = rules => List(new RulesetMetric("ruleset", rules))
            val res = Once executeTask new RdfRulesKgcMiningTask(taskName, rulesCache.getAbsolutePath, numberOfThreads, ruleLength, anytime, constants) withInput index
            res andFinallyProcessAndReturnResultWith BasicPrinter()
          } else {
            val res = Ruleset(index, rulesCache).withDebugger().cache
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
            val res = Ruleset(index, rulesCache).withDebugger().cache
            (taskName, res, List(new RulesetMetric("ruleset", res))) andFinallyProcessAndReturnResultWith BasicPrinter()
          }
        }

        lazy val exactRuleset = mineRules("")
        lazy val exactConfRuleset = computeConfidence("", exactRuleset)(DefaultConfidence(Measure.QpcaConfidence))

        if (cli.hasOption("runconfidences")) {
          val ruleset = exactRuleset
          Functor(DefaultConfidence(Measure.CwaConfidence)).foreach { implicit defaultConfidence =>
            val confRuleset = computeConfidence("", ruleset)
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, CWA, Joint", test, NoisyOrScorer(), TopRules(100)) with PredictionTaskPostprocessor withInput confRuleset andFinallyProcessResultWith BasicPrinter()
          }
          Functor(DefaultConfidence(Measure.PcaConfidence)).foreach { implicit defaultConfidence =>
            val confRuleset = computeConfidence("", ruleset)
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, PCA, Joint", test, NoisyOrScorer(), TopRules(100)) with PredictionTaskPostprocessor withInput confRuleset andFinallyProcessResultWith BasicPrinter()
          }
          Functor(DefaultConfidence(Measure.QpcaConfidence)).foreach { implicit defaultConfidence =>
            val confRuleset = exactConfRuleset
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, QPCA, Joint", test, NoisyOrScorer(), TopRules(100)) with PredictionTaskPostprocessor withInput confRuleset andFinallyProcessResultWith BasicPrinter()
          }
        }
        if (cli.hasOption("runmodes")) {
          val ruleset = exactConfRuleset
          implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.QpcaConfidence)
          implicit val toMetrics: PredictionTasksResults => Seq[Metric] = _ => Nil
          val predictions = Once executeTask new PredictionTask[PredictionTasksResults]("RDFRules prediction task, modes", test, NoisyOrScorer(), TopRules(100)) with TaskPostProcessor[PredictionTasksResults, PredictionTasksResults] {
            protected def postProcess(result: PredictionTasksResults): PredictionTasksResults = result
          } withInput ruleset andFinallyProcessAndReturnResultWith BasicPrinter()
          Once executeTask new ReadyPredictionTask[Seq[Metric]]("RDFRules prediction with modes") with ModesPredictionTaskPostprocessor withInput predictions andFinallyProcessResultWith BasicPrinter()
          Once executeTask new ReadyPredictionTask[Seq[Metric]]("RDFRules prediction without modes") with PredictionTaskPostprocessor withInput predictions andFinallyProcessResultWith BasicPrinter()
        }
        if (cli.hasOption("runconstants")) {
          implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.QpcaConfidence)
          Functor(mineRules("noconstants", constants = ConstantsAtPosition.ConstantsPosition.Nowhere)).map { ruleset =>
            computeConfidence("noconstants", ruleset)
          }.foreach { ruleset =>
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, NoConstants", test, NoisyOrScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          }
          Functor(mineRules("constantslower", constants = ConstantsAtPosition.ConstantsPosition.LowerCardinalitySide())).map { ruleset =>
            computeConfidence("constantslower", ruleset)
          }.foreach { ruleset =>
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, ConstantsLowerCadinalitySide", test, NoisyOrScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          }
        }
        if (cli.hasOption("runanytime")) {
          implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.QpcaConfidence)
          Functor(mineRules("anytime", true)).map { ruleset =>
            computeConfidence("anytime", ruleset)
          }.foreach { ruleset =>
            Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, Anytime", test, NoisyOrScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          }
        }
        if (cli.hasOption("runscorers")) {
          implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.QpcaConfidence)
          val ruleset = exactConfRuleset
          Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, Maximum", test, MaximumScorer(), TopRules(100)) with ModesPredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, NonRedundantNoisyOr", test, NoisyOrScorer(), NonRedundantTopRules(100)) with ModesPredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
        }

        /** if (cli.hasOption("runlift")) {
          * val ruleset = {
          * implicit val rulesToMetrics: Ruleset => Seq[Metric] = rules => List(new RulesetMetric("ruleset", rules))
          * val res = Once executeTask new RdfRulesKgcMiningTask(s"RDFRules mining", numberOfThreads, 4, false) withInput index
          * res andFinallyProcessAndReturnResultWith BasicPrinter()
          * }
          * {
          * implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.CwaConfidence)
          * Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, CWA, Joint", test, JointScorer()) with PredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          * }
          * {
          * implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.PcaConfidence)
          * Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, PCA, Joint", test, JointScorer()) with PredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          * }
          * {
          * implicit val defaultConfidence: DefaultConfidence = DefaultConfidence(Measure.QpcaConfidence)
          * Once executeTask new PredictionTask[Seq[Metric]]("RDFRules prediction task, QPCA, Joint", test, JointScorer()) with PredictionTaskPostprocessor withInput ruleset andFinallyProcessResultWith BasicPrinter()
          * }
          * } */
      }
    } finally {
      outputWriter.close()
    }
    HowLong.flushAllResults()
  }

}