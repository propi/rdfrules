package com.github.propi.rdfrules.experiments

import java.io.File

import org.apache.commons.io.FileUtils

/**
  * Created by Vaclav Zeman on 25. 7. 2018.
  */
trait Example[T] {

  def name: String

  protected def example: T

  final def execute(implicit resultPrinter: ResultPrinter[T]): Unit = {
    println("*****************************************")
    println(s"Example: $name")
    println("*****************************************")
    val result = example
    println("-----------------------------------------")
    println(s"Result of this example for: ${resultPrinter.name}")
    println("-----------------------------------------")
    resultPrinter.printResult(result)
    println("+++++++++++++++++++++++++++++++++++++++++")
    println("+++++++++++++++++++++++++++++++++++++++++")
  }

}

object Example {

  val resultDir = "experiments/results/"
  val experimentsDir = "experiments/data/"

  def prepareResultsDir(): Unit = {
    val resultsDir = new File(resultDir.stripSuffix("/"))
    if (!resultsDir.isDirectory) resultsDir.mkdirs()
    FileUtils.cleanDirectory(resultsDir)
  }

}