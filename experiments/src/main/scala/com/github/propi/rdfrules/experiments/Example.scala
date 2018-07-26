package com.github.propi.rdfrules.experiments

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