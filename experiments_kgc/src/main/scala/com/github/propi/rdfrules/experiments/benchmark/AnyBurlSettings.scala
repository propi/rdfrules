package com.github.propi.rdfrules.experiments.benchmark

import java.io.{File, PrintWriter}

case class AnyBurlSettings(pathTrain: String,
                           pathValid: String,
                           pathTest: String,
                           outputFolder: String,
                           snapshotsAt: Int,
                           minSupportThreshold: Int,
                           parallelism: Int,
                           rlen: Int,
                           eval: Boolean) {


  private def outputPath: String = if (eval) s"$outputFolder/preds-|X|" else s"$outputFolder/rules"

  def saveConfigTo(path: String): File = {
    val pw = new PrintWriter(path, "UTF-8")
    try {
      pw.println(s"PATH_TRAINING = $pathTrain")
      if (eval) {
        pw.println(s"PATH_VALID = $pathValid")
        pw.println(s"PATH_TEST = $pathTest")
      }
      pw.println(s"PATH_OUTPUT = $outputPath")
      pw.println(s"SNAPSHOTS_AT = $snapshotsAt")
      pw.println(s"THRESHOLD_CORRECT_PREDICTIONS = $minSupportThreshold")
      pw.println(s"BATCH_TIME = 2000")
      pw.println(s"WORKER_THREADS = $parallelism")
      pw.println(s"PATH_PREDICTIONS = $outputFolder/preds-$snapshotsAt")
      pw.println(s"PATH_RULES = $outputFolder/rules-$snapshotsAt")
      pw.println(s"THRESHOLD_CONFIDENCE = 0.1")
      pw.println(s"MAX_LENGTH_CYCLIC = ${rlen - 1}")
    } finally {
      pw.close()
    }
    new File(path)
  }

  def rulesFile: File = new File(s"$outputFolder/rules-$snapshotsAt")

}