package com.github.propi.rdfrules.java

import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition

/**
  * Created by Vaclav Zeman on 30. 4. 2020.
  */
object ConstantsPosition {

  def objectPosition: ConstantsAtPosition.ConstantsPosition = ConstantsAtPosition.ConstantsPosition.Object

  def subjectPosition: ConstantsAtPosition.ConstantsPosition = ConstantsAtPosition.ConstantsPosition.Subject

  def leastFunctionalVariable: ConstantsAtPosition.ConstantsPosition = ConstantsAtPosition.ConstantsPosition.LeastFunctionalVariable

  def nowhere: ConstantsAtPosition.ConstantsPosition = ConstantsAtPosition.ConstantsPosition.Nowhere

}