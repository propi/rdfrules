package com.github.propi.rdfrules.experiments

import java.io.{File, FileInputStream, FileOutputStream, PrintStream}

import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.data.{Graph, TripleItem}
import com.github.propi.rdfrules.experiments.benchmark.Benchmark._
import com.github.propi.rdfrules.experiments.benchmark.MetricResultProcessor.BasicPrinter
import com.github.propi.rdfrules.experiments.benchmark.MetricsAggregator.StatsAggregator
import com.github.propi.rdfrules.experiments.benchmark.{ClusterDistancesTaskPostprocessor, DiscretizedRuleFilter, Metric, NewTriplesPostprocessor, RulesTaskPostprocessor}
import com.github.propi.rdfrules.experiments.benchmark.tasks._
import com.github.propi.rdfrules.index.TripleHashIndex
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule.{AtomPattern, RuleConstraint, Threshold}
import com.github.propi.rdfrules.ruleset.ResolvedRule
import com.github.propi.rdfrules.utils.{Debugger, HowLong}
import org.apache.commons.cli.{Options, PosixParser}
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.IOUtils

import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 7. 5. 2019.
  */
object OriginalAmieComparison {

  private def getInputTsvDataset(inputDataset: String): String = {
    if (inputDataset.endsWith(".bz2")) {
      val fileExtracted = inputDataset.stripSuffix(".bz2")
      if (!new File(fileExtracted).isFile) {
        println("file uncompressing...")
        val inputS = new BZip2CompressorInputStream(new FileInputStream(inputDataset))
        val outputS = new FileOutputStream(fileExtracted)
        try {
          IOUtils.copyLarge(inputS, outputS)
        } finally {
          inputS.close()
          outputS.close()
        }
      }
      fileExtracted
    } else if (inputDataset.endsWith(".gz")) {
      val fileExtracted = inputDataset.stripSuffix(".gz")
      if (!new File(fileExtracted).isFile) {
        println("file uncompressing...")
        val inputS = new GzipCompressorInputStream(new FileInputStream(inputDataset))
        val outputS = new FileOutputStream(fileExtracted)
        try {
          IOUtils.copyLarge(inputS, outputS)
        } finally {
          inputS.close()
          outputS.close()
        }
      }
      fileExtracted
    } else {
      inputDataset
    }
  }

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
    options.addOption("coresc", true, "list of cores")
    options.addOption("minhcs", true, "list of min head coverages")
    options.addOption("minsims", true, "list of min similarities for clustering")
    options.addOption("topks", true, "list of topK for pruning")
    options.addOption("input", true, "input TSV dataset")
    options.addOption("output", true, "output file")
    options.addOption("times", true, "number of repetition of each task")

    options.addOption("runtopk", false, "run top-k test")
    options.addOption("runconstants", false, "run mining with constants test")
    options.addOption("runlogical", false, "run mining only logical rules test")
    options.addOption("runcores", false, "run scalability test")
    options.addOption("runpatterns", false, "run patterns test")
    options.addOption("runconfidence", false, "run confidence counting test")
    options.addOption("runclusters", false, "run clusters test")
    options.addOption("rundiscretization", false, "run discretization test")
    options.addOption("runpruning", false, "run pruning test")
    options.addOption("rungraphs", false, "run graph-aware mining test")

    options.addOption("rdfrulesonly", false, "run only rdf rules tests")
    options.addOption("amieonly", false, "run only amie+ tests")

    println(s"Number of cores: ${Runtime.getRuntime.availableProcessors()}")

    val cli = parser.parse(options, args)

    val numberOfThreads = cli.getOptionValue("cores", Runtime.getRuntime.availableProcessors().toString).toInt
    val listOfCores: Seq[Int] = {
      val x = cli.getOptionValue("coresc", "").split(",").iterator.map(_.trim).filter(_.nonEmpty).map(_.toInt).toList
      if (x.isEmpty) 1 to numberOfThreads else x
    }
    val minHcs = cli.getOptionValue("minhcs", "0.005,0.01,0.02,0.05,0.1,0.2,0.3").split(",").iterator.map(_.trim).filter(_.nonEmpty).map(_.toDouble).toList
    val inputTsvDataset = getInputTsvDataset(cli.getOptionValue("input", "experiments/data/yago2core_facts.clean.notypes.tsv.bz2"))
    //experiments/data/mappingbased_objects_sample.tsv.bz2
    val outputFile = cli.getOptionValue("output", "experiments/data/results.txt")
    val xTimes = cli.getOptionValue("times", "7").toInt times
    implicit val outputWriter: PrintStream = new PrintStream(outputFile)
    try {
      Debugger() { implicit debugger =>
        if (cli.hasOption("rungraphs")) {
          val yago1 = Graph("experiments/data/yagoFacts.tsv.bz2")
          val yago2 = Graph("experiments/data/yagoLiteralFacts.tsv.bz2")
          val dbpedia1 = Graph("experiments/data/mappingbased_literals_sample.ttl.bz2")
          val dbpedia2 = Graph("experiments/data/mappingbased_objects_sample.tsv.bz2")
          val yagoDbpedia = Graph("experiments/data/yagoDBpediaInstances.tsv.bz2")
          Once executeTask new MinHcRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: graphs-mining YAGO, minHeadCoverage = 0.01", 0.01, true, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor {
            override val withConstantsAtTheObjectPosition: Boolean = true
          } withInput (yago1.toDataset + yago2).index() andFinallyProcessResultWith BasicPrinter()
          Once executeTask new MinHcRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: graphs-mining DBpedia, minHeadCoverage = 0.01", 0.01, true, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor {
            override val withConstantsAtTheObjectPosition: Boolean = true
          } withInput (dbpedia1.toDataset + dbpedia2).index() andFinallyProcessResultWith BasicPrinter()
          Once executeTask new MinHcRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: graphs-mining YAGO+DBpedia, minHeadCoverage = 0.01", 0.01, true, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor {
            override val withConstantsAtTheObjectPosition: Boolean = true
          } withInput (yago1.toDataset + yago2 + dbpedia1 + dbpedia2 + yagoDbpedia).index() andFinallyProcessResultWith BasicPrinter()
        }/* else if (cli.hasOption("runlift")) {
          val yago = Graph("experiments/data/yago2core_facts.clean.notypes.tsv.bz2")
          val dbpedia = Graph("experiments/data/dbpedia.3.8.tsv.bz2")
          val yagoDbpedia = Graph("experiments/data/yagoDBpediaInstancesAll.tsv.bz2")
        }*/ else if (cli.hasOption("rundiscretization")) {
          for (minHc <- minHcs) {
            val index = Graph(inputTsvDataset).index()
            index.tripleMap(thi => thi.asInstanceOf[TripleHashIndex[Int]].reset())
            Once executeTask new MinHcRdfRules[Seq[Metric]](s"RDFRules: mine without discretization, minHc: $minHc", minHc, true, numberOfThreads = numberOfThreads) with NewTriplesPostprocessor {
              override val withConstantsAtTheObjectPosition: Boolean = true
              override val minPcaConfidence: Double = 0.0
              override val minConfidence: Double = 0.0
            } withInput index andFinallyProcessResultWith BasicPrinter()
            val updatedIndex = Once executeTask new DiscretizationRdfRules(s"RDFRules: discretization, minHc = $minHc", minHc) withInput index andFinallyProcessAndReturnResultWith BasicPrinter()
            Once executeTask new DiscretizationMiningRdfRules[Seq[Metric]](s"RDFRules: mine with discretization, minHc: $minHc", DiscretizedRuleFilter(updatedIndex), minHc, numberOfThreads) with NewTriplesPostprocessor withInput updatedIndex andFinallyProcessResultWith BasicPrinter()
          }
        } else {
          lazy val index = {
            val index = Graph(inputTsvDataset).index().withEvaluatedLazyVals
            HowLong.howLong("RDFRules indexing", memUsage = true, forceShow = true) {
              index.tripleMap(_.size)
            }
            index
          }
          if (cli.hasOption("runlogical")) {
            for (minHc <- minHcs) {
              val taskDesc = s"minHeadCoverage = $minHc, minConfidence = 0.1, minPcaConfidence = 0.1, only logical rules"
              val taskResult1 = if (!cli.hasOption("rdfrulesonly")) Some(
                xTimes executeTask new MinHcAmie(s"AMIE: $taskDesc", minHc, numberOfThreads = numberOfThreads) withInput inputTsvDataset andAggregateResultWith StatsAggregator
              ) else None
              val taskResult2 = if (!cli.hasOption("amieonly")) Some(
                xTimes executeTask new MinHcRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", minHc, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor withInput index andAggregateResultWith StatsAggregator
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
              val taskDesc = s"minHeadCoverage = $minHc, minConfidence = 0.1, minPcaConfidence = 0.1, with constants"
              val taskResult1 = if (!cli.hasOption("rdfrulesonly")) Some(
                xTimes executeTask new MinHcAmie(s"AMIE: $taskDesc", minHc, true, numberOfThreads = numberOfThreads) withInput inputTsvDataset andAggregateResultWith StatsAggregator
              ) else None
              val taskResult2 = if (!cli.hasOption("amieonly")) Some(
                xTimes executeTask new MinHcRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", minHc, true, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor withInput index andAggregateResultWith StatsAggregator
              ) else None
              (taskResult1, taskResult2) match {
                case (Some(tr1), Some(tr2)) => tr1 compareWith tr2 andFinallyProcessResultWith BasicPrinter()
                case (Some(tr1), _) => tr1 andFinallyProcessResultWith BasicPrinter()
                case (_, Some(tr2)) => tr2 andFinallyProcessResultWith BasicPrinter()
                case _ =>
              }
            }
          }
          if (cli.hasOption("runcores")) {
            for (cores <- listOfCores) {
              val taskDesc = s"cores = $cores, minHeadCoverage = ${minHcs.head}, minConfidence = 0.1, minPcaConfidence = 0.1, only logical rules"
              val taskResult1 = if (!cli.hasOption("rdfrulesonly")) Some(
                xTimes executeTask new NumOfThreadsAmie(s"AMIE: $taskDesc", cores, minHcs.head) withInput inputTsvDataset andAggregateResultWith StatsAggregator
              ) else None
              val taskResult2 = if (!cli.hasOption("amieonly")) Some(
                xTimes executeTask new NumOfThreadsRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", cores, minHcs.head) with RulesTaskPostprocessor withInput index andAggregateResultWith StatsAggregator
              ) else None
              (taskResult1, taskResult2) match {
                case (Some(tr1), Some(tr2)) => tr1 compareWith tr2 andFinallyProcessResultWith BasicPrinter()
                case (Some(tr1), _) => tr1 andFinallyProcessResultWith BasicPrinter()
                case (_, Some(tr2)) => tr2 andFinallyProcessResultWith BasicPrinter()
                case _ =>
              }
            }
            for (cores <- listOfCores) {
              val taskDesc = s"cores = $cores, minHeadCoverage = ${minHcs.head}, minConfidence = 0.1, minPcaConfidence = 0.1, with constants"
              val taskResult1 = if (!cli.hasOption("rdfrulesonly")) Some(
                xTimes executeTask new NumOfThreadsAmie(s"AMIE: $taskDesc", cores, minHcs.head, true) withInput inputTsvDataset andAggregateResultWith StatsAggregator
              ) else None
              val taskResult2 = if (!cli.hasOption("amieonly")) Some(
                xTimes executeTask new NumOfThreadsRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", cores, minHcs.head, true) with RulesTaskPostprocessor withInput index andAggregateResultWith StatsAggregator
              ) else None
              (taskResult1, taskResult2) match {
                case (Some(tr1), Some(tr2)) => tr1 compareWith tr2 andFinallyProcessResultWith BasicPrinter()
                case (Some(tr1), _) => tr1 andFinallyProcessResultWith BasicPrinter()
                case (_, Some(tr2)) => tr2 andFinallyProcessResultWith BasicPrinter()
                case _ =>
              }
            }
          }
          if (cli.hasOption("runtopk")) {
            for (minHc <- minHcs) {
              val taskDesc = s"top 100 with highest head coverage, minHeadCoverage = $minHc, only logical rules"
              xTimes executeTask new TopKRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", 100, minHc, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor withInput index andAggregateResultWith StatsAggregator andFinallyProcessResultWith BasicPrinter()
            }
            for (minHc <- minHcs) {
              val taskDesc = s"top 100 with highest head coverage, minHeadCoverage = $minHc, with constants"
              xTimes executeTask new TopKRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", 100, minHc, true, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor withInput index andAggregateResultWith StatsAggregator andFinallyProcessResultWith BasicPrinter()
            }
          }
          if (cli.hasOption("runpatterns")) {
            xTimes executeTask new PatternRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: mining with pattern ? -> hasAcademicAdvisor, minHeadCoverage = 0.01, with constants", allowConstants = true, numberOfThreads = numberOfThreads)(AtomPattern(predicate = TripleItem.Uri("hasAcademicAdvisor"))) with RulesTaskPostprocessor {
              override val minPcaConfidence: Double = 0.0
              override val minConfidence: Double = 0.0
            } withInput index andAggregateResultWith StatsAggregator andFinallyProcessResultWith BasicPrinter()
            xTimes executeTask new PatternRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: mining with pattern hasWonPrize -> ?, minHeadCoverage = 0.01, with constants", allowConstants = true, numberOfThreads = numberOfThreads)(AtomPattern(predicate = TripleItem.Uri("hasWonPrize")) =>: Option.empty[AtomPattern]) with RulesTaskPostprocessor {
              override val minPcaConfidence: Double = 0.0
              override val minConfidence: Double = 0.0
            } withInput index andAggregateResultWith StatsAggregator andFinallyProcessResultWith BasicPrinter()
            xTimes executeTask new MinHcRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: mine all, minHeadCoverage = 0.01, with constants", 0.01, true, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor {
              override val minPcaConfidence: Double = 0.0
              override val minConfidence: Double = 0.0
            } withInput index andAggregateResultWith StatsAggregator andFinallyProcessResultWith BasicPrinter()
          }
          if (cli.hasOption("runconfidence")) {
            val rules = index.mine(Amie().setParallelism(numberOfThreads).addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Object)).addThreshold(Threshold.MinHeadCoverage(0.01)).addThreshold(Threshold.TopK(100000))).cache
            xTimes executeTask new ConfidenceRdfRules[IndexedSeq[ResolvedRule]]("RDFRules: confidence counting, minPcaConfidence=0.1, input 100000 rules with constants", 0, 0.1, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor withInput rules andAggregateResultWith StatsAggregator andFinallyProcessResultWith BasicPrinter()
            xTimes executeTask new ConfidenceRdfRules[IndexedSeq[ResolvedRule]]("RDFRules: confidence counting, minConfidence=0.1, input 100000 rules with constants", 0.1, 0.0, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor withInput rules andAggregateResultWith StatsAggregator andFinallyProcessResultWith BasicPrinter()
            xTimes executeTask new ConfidenceRdfRules[IndexedSeq[ResolvedRule]]("RDFRules: confidence counting, minPcaConfidence=0.1, input 100000 rules with constants, topK=100", 0, 0.1, topK = 100, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor withInput rules andAggregateResultWith StatsAggregator andFinallyProcessResultWith BasicPrinter()
            xTimes executeTask new ConfidenceRdfRules[IndexedSeq[ResolvedRule]]("RDFRules: confidence counting, minConfidence=0.1, input 100000 rules with constants, topK=100", 0.1, 0.0, topK = 100, numberOfThreads = numberOfThreads) with RulesTaskPostprocessor withInput rules andAggregateResultWith StatsAggregator andFinallyProcessResultWith BasicPrinter()
          }
          if (cli.hasOption("runclusters")) {
            val rules = index.mine(Amie().setParallelism(numberOfThreads).addThreshold(Threshold.MinHeadCoverage(0.01)).addThreshold(Threshold.TopK(10000))).cache
            val sims = cli.getOptionValue("minsims", "0.1,0.2,0.3,0.4,0.5,0.6,0.7,0.8").split(",").iterator.map(_.trim).filter(_.nonEmpty).map(_.toDouble).toList
            for (minSim <- sims) {
              Once executeTask new ClusteringRdfRules[Seq[Metric]](s"RDFRules: clustering, minSim = $minSim", 1, minSim, numberOfThreads) with ClusterDistancesTaskPostprocessor withInput rules andFinallyProcessResultWith BasicPrinter()
            }
          }
          if (cli.hasOption("runpruning")) {
            val topKs = cli.getOptionValue("topks", "500, 1000, 2000, 4000, 8000, 16000, 32000").split(",").iterator.map(_.trim).filter(_.nonEmpty).map(_.toInt).toList
            for (topK <- topKs) {
              val rules = index.mine(Amie().setParallelism(numberOfThreads).addThreshold(Threshold.MinHeadCoverage(0.01)).addThreshold(Threshold.TopK(topK))).computeConfidence(0.1).cache
              Once executeTask new PruningRdfRules(s"RDFRules: pruning, rules: ${rules.size}, topK: $topK") withInput rules andFinallyProcessResultWith BasicPrinter()
            }
          }
          //mined rules
          /*
  (?b <influences> ?a) -> (?a <hasAcademicAdvisor> ?b) | support: 29, headCoverage: 0.014894709809964048, headSize: 1947
  (?a <isCitizenOf> <Germany>) ^ (?b <isCitizenOf> <Germany>) -> (?a <hasAcademicAdvisor> ?b) | support: 38, headCoverage: 0.019517205957883924, headSize: 1947
  (?a <livesIn> <Germany>) ^ (?b <isCitizenOf> <Germany>) -> (?a <hasAcademicAdvisor> ?b) | support: 22, headCoverage: 0.011299435028248588, headSize: 1947
  (?b <hasWonPrize> <Nobel_Prize_in_Physics>) ^ (?a <hasWonPrize> <Nobel_Prize_in_Physics>) -> (?a <hasAcademicAdvisor> ?b) | support: 31, headCoverage: 0.015921931176168466, headSize: 1947
  (?b <isCitizenOf> ?c) ^ (?a <livesIn> ?c) -> (?a <hasAcademicAdvisor> ?b) | support: 39, headCoverage: 0.020030816640986132, headSize: 1947
  (?b <livesIn> ?c) ^ (?a <livesIn> ?c) -> (?a <hasAcademicAdvisor> ?b) | support: 38, headCoverage: 0.019517205957883924, headSize: 1947
  (?b <hasWonPrize> ?c) ^ (?a <hasWonPrize> ?c) -> (?a <hasAcademicAdvisor> ?b) | support: 85, headCoverage: 0.04365690806368772, headSize: 1947
  (?a <isCitizenOf> ?c) ^ (?b <isCitizenOf> ?c) -> (?a <hasAcademicAdvisor> ?b) | support: 103, headCoverage: 0.05290190035952748, headSize: 1947
  (?a <graduatedFrom> ?c) ^ (?b <graduatedFrom> ?c) -> (?a <hasAcademicAdvisor> ?b) | support: 45, headCoverage: 0.023112480739599383, headSize: 1947
  (?a <graduatedFrom> ?c) ^ (?b <worksAt> ?c) -> (?a <hasAcademicAdvisor> ?b) | support: 100, headCoverage: 0.05136106831022085, headSize: 1947
  (?a <worksAt> ?c) ^ (?b <worksAt> ?c) -> (?a <hasAcademicAdvisor> ?b) | support: 51, headCoverage: 0.026194144838212634, headSize: 1947
  (?a <isCitizenOf> ?c) ^ (?b <livesIn> ?c) -> (?a <hasAcademicAdvisor> ?b) | support: 38, headCoverage: 0.019517205957883924, headSize: 1947
  (?b <diedIn> ?c) ^ (?a <diedIn> ?c) -> (?a <hasAcademicAdvisor> ?b) | support: 26, headCoverage: 0.01335387776065742, headSize: 1947

  //index.mine(Amie().addConstraint(RuleConstraint.WithInstances(true)).addPattern(AtomPattern(predicate = TripleItem.Uri("hasWonPrize")) =>: Option.empty[AtomPattern]))
  (?a <hasWonPrize> <Purple_Heart>) -> (?a <hasWonPrize> <Medal_of_Honor>) | support: 507, headCoverage: 0.014910449078022527, pcaConfidence: 0.5090361445783133, headSize: 34003, pcaBodySize: 996
  (?b <isMarriedTo> ?a) ^ (?b <hasWonPrize> <Emmy_Award>) -> (?a <isMarriedTo> ?b) | support: 179, headCoverage: 0.014925373134328358, pcaConfidence: 0.8861386138613861, headSize: 11993, pcaBodySize: 202
  (?b <isMarriedTo> ?a) ^ (?a <hasWonPrize> <Emmy_Award>) -> (?a <isMarriedTo> ?b) | support: 179, headCoverage: 0.014925373134328358, pcaConfidence: 0.9835164835164835, headSize: 11993, pcaBodySize: 182
  (?a <hasWonPrize> <Legion_of_Merit>) -> (?a <hasWonPrize> <Bronze_Star_Medal>) | support: 445, headCoverage: 0.013087080551716024, pcaConfidence: 0.3662551440329218, headSize: 34003, pcaBodySize: 1215
  (?a <hasWonPrize> <Knight's_Cross_of_the_Iron_Cross>) -> (?a <hasWonPrize> <Iron_Cross>) | support: 374, headCoverage: 0.010999029497397289, pcaConfidence: 0.2039258451472192, headSize: 34003, pcaBodySize: 1834
  (?a <hasWonPrize> <Bronze_Star_Medal>) -> (?a <hasWonPrize> <Legion_of_Merit>) | support: 445, headCoverage: 0.013087080551716024, pcaConfidence: 0.6294200848656294, headSize: 34003, pcaBodySize: 707
  (?a <hasWonPrize> <Medal_of_Honor>) -> (?a <hasWonPrize> <Purple_Heart>) | support: 507, headCoverage: 0.014910449078022527, pcaConfidence: 0.2542627883650953, headSize: 34003, pcaBodySize: 1994
  (?a <hasWonPrize> <Iron_Cross>) -> (?a <hasWonPrize> <Knight's_Cross_of_the_Iron_Cross>) | support: 374, headCoverage: 0.010999029497397289, pcaConfidence: 0.554074074074074, headSize: 34003, pcaBodySize: 675
           */
        }
      }
    } finally {
      outputWriter.close()
    }
    HowLong.flushAllResults()
  }

}