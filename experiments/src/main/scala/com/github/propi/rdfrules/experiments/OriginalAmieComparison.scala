package com.github.propi.rdfrules.experiments

import java.io.{BufferedInputStream, File, FileInputStream, FileOutputStream, PrintStream}

import com.github.propi.rdfrules.data.Graph
import com.github.propi.rdfrules.experiments.benchmark.Benchmark._
import com.github.propi.rdfrules.experiments.benchmark.MetricResultProcessor.BasicPrinter
import com.github.propi.rdfrules.experiments.benchmark.MetricsAggregator.StatsAggregator
import com.github.propi.rdfrules.experiments.benchmark.RulesTaskPostprocessor
import com.github.propi.rdfrules.experiments.benchmark.tasks.{MinHcAmie, MinHcRdfRules, NumOfThreadsAmie, NumOfThreadsRdfRules, TopKRdfRules}
import com.github.propi.rdfrules.ruleset.ResolvedRule
import com.github.propi.rdfrules.utils.{Debugger, HowLong}
import org.apache.commons.cli.{DefaultParser, Options, PosixParser}
import org.apache.commons.compress.compressors.CompressorStreamFactory
import org.apache.commons.compress.compressors.bzip2.{BZip2CompressorInputStream, BZip2Utils}
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.IOUtils

import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 7. 5. 2019.
  */
object OriginalAmieComparison {

  def main(args: Array[String]): Unit = {
    val parser = new PosixParser
    val options = new Options
    options.addOption("cores", true, "max number of cores")
    options.addOption("input", true, "input TSV dataset")
    options.addOption("output", true, "output file")
    options.addOption("times", true, "number of repetition of each task")
    val cli = parser.parse(options, args)
    val maxNumOfCores = cli.getOptionValue("cores", Runtime.getRuntime.availableProcessors().toString).toInt
    val inputTsvDataset = {
      //experiments/data/mappingbased_objects_sample.tsv.bz2
      val file = cli.getOptionValue("input", "experiments/data/yago2core_facts.clean.notypes.tsv.bz2")
      if (file.endsWith(".bz2")) {
        val fileExtracted = file.stripSuffix(".bz2")
        if (!new File(fileExtracted).isFile) {
          println("file uncompressing...")
          val inputS = new BZip2CompressorInputStream(new FileInputStream(file))
          val outputS = new FileOutputStream(fileExtracted)
          try {
            IOUtils.copyLarge(inputS, outputS)
          } finally {
            inputS.close()
            outputS.close()
          }
        }
        fileExtracted
      } else if (file.endsWith(".gz")) {
        println("file uncompressing...")
        val fileExtracted = file.stripSuffix(".gz")
        if (!new File(fileExtracted).isFile) {
          println("file uncompressing...")
          val inputS = new GzipCompressorInputStream(new FileInputStream(file))
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
        file
      }
    }
    val outputFile = cli.getOptionValue("output", "experiments/data/results.txt")
    val xTimes = cli.getOptionValue("times", "6").toInt times
    implicit val outputWriter: PrintStream = new PrintStream(outputFile)
    try {
      Debugger() { implicit debugger =>
        val index = Graph(inputTsvDataset).index().withEvaluatedLazyVals
        HowLong.howLong("RDFRules indexing", memUsage = true, forceShow = true) {
          index.tripleMap(_.size)
        }
        val minHcs = List(0.005, 0.01, 0.02, 0.05, 0.1, 0.2, 0.3)
        /*for (minHc <- minHcs) {
          val taskDesc = s"minHeadCoverage = $minHc, minConfidence = 0.1, minPcaConfidence = 0.1, only logical rules"
          val taskResult1 = xTimes executeTask new MinHcAmie(s"AMIE: $taskDesc", minHc) withInput inputTsvDataset andAggregateResultWith StatsAggregator
          val taskResult2 = xTimes executeTask new MinHcRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", minHc) with RulesTaskPostprocessor withInput index andAggregateResultWith StatsAggregator
          taskResult1 compareWith taskResult2 andFinallyProcessResultWith BasicPrinter()
        }
        for (minHc <- minHcs) {
          val taskDesc = s"minHeadCoverage = $minHc, minConfidence = 0.1, minPcaConfidence = 0.1, with constants"
          val taskResult1 = xTimes executeTask new MinHcAmie(s"AMIE: $taskDesc", minHc, true) withInput inputTsvDataset andAggregateResultWith StatsAggregator
          val taskResult2 = xTimes executeTask new MinHcRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", minHc, true) with RulesTaskPostprocessor withInput index andAggregateResultWith StatsAggregator
          taskResult1 compareWith taskResult2 andFinallyProcessResultWith BasicPrinter()
        }
        for (cores <- 1 to maxNumOfCores) {
          val taskDesc = s"cores = $cores, minHeadCoverage = 0.01, minConfidence = 0.1, minPcaConfidence = 0.1, only logical rules"
          val taskResult1 = xTimes executeTask new NumOfThreadsAmie(s"AMIE: $taskDesc", cores) withInput inputTsvDataset andAggregateResultWith StatsAggregator
          val taskResult2 = xTimes executeTask new NumOfThreadsRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", cores) with RulesTaskPostprocessor withInput index andAggregateResultWith StatsAggregator
          taskResult1 compareWith taskResult2 andFinallyProcessResultWith BasicPrinter()
        }*/
        /*for (minHc <- minHcs) {
          val taskDesc = s"top 100 with highest head coverage, minHeadCoverage = $minHc, only logical rules"
          xTimes executeTask new TopKRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", 100, minHc) with RulesTaskPostprocessor withInput index andAggregateResultWith StatsAggregator andFinallyProcessResultWith BasicPrinter()
        }*/
        for (minHc <- minHcs) {
          val taskDesc = s"top 100 with highest head coverage, minHeadCoverage = $minHc, with constants"
          xTimes executeTask new TopKRdfRules[IndexedSeq[ResolvedRule]](s"RDFRules: $taskDesc", 100, minHc, true) with RulesTaskPostprocessor withInput index andAggregateResultWith StatsAggregator andFinallyProcessResultWith BasicPrinter()
        }
      }
    } finally {
      outputWriter.close()
    }
    HowLong.flushAllResults()
  }

}