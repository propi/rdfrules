package com.github.propi.rdfrules.experiments.benchmark.tasks

import amie.mining.AMIE
import com.github.propi.rdfrules.experiments.benchmark.{AmieRulesMiningTask, DefaultMiningSettings}
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition

/**
  * Created by Vaclav Zeman on 17. 5. 2019.
  */
class PatternAmie[T](val name: String,
                     val bodyRelations: Set[String],
                     val headRelations: Set[String],
                     override val minHeadCoverage: Double,
                     override val allowConstants: Option[ConstantsPosition] = Some(ConstantsPosition.Nowhere),
                     override val numberOfThreads: Int = DefaultMiningSettings.numberOfThreads) extends AmieRulesMiningTask {

  override protected def preProcess(input: String): AMIE = {
    val btr = bodyRelations.mkString(",")
    val htr = headRelations.mkString(",")
    val additionalCmd = List(
      if (btr.isEmpty) "" else s"-btr $btr ",
      if (htr.isEmpty) "" else s"-htr $htr "
    ).mkString
    super.preProcess(additionalCmd + input)
  }

}