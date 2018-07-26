package com.github.propi.rdfrules.experiments

import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 25. 7. 2018.
  */
trait ResultPrinter[T] {
  def name: String

  def printResult(x: T): Unit
}

object ResultPrinter {

  implicit object RulesetPrinter extends ResultPrinter[Ruleset] {
    val name: String = "Ruleset"

    def printResult(x: Ruleset): Unit = {
      println("Number of found rules: " + x.size)
      println("Rules sample:")
      x.resolvedRules.foreach(println)
    }
  }

}
