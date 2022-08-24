package com.github.propi.rdfrules

import com.github.propi.rdfrules.rule.AtomPattern.AtomItemPattern

/**
  * Created by Vaclav Zeman on 4. 8. 2018.
  */
package object rule {

  implicit class PimpedOptionalConsequent(protected val consequent: Option[AtomPattern]) extends AtomPattern.PimpedOptionalConsequent

  implicit class PimpedConsequent(consequent: AtomPattern) extends PimpedOptionalConsequent(Some(consequent))

  def AnyVariable: AtomItemPattern.AnyVariable.type = AtomItemPattern.AnyVariable

  def AnyConstant: AtomItemPattern.AnyConstant.type = AtomItemPattern.AnyConstant

  def Any: AtomItemPattern.Any.type = AtomItemPattern.Any

  def OneOf(atomItemPattern: AtomItemPattern.Constant, atomItemPatterns: AtomItemPattern.Constant*): AtomItemPattern.OneOf = AtomItemPattern.OneOf(atomItemPattern, atomItemPatterns: _*)

  def NoneOf(atomItemPattern: AtomItemPattern.Constant, atomItemPatterns: AtomItemPattern.Constant*): AtomItemPattern.NoneOf = AtomItemPattern.NoneOf(atomItemPattern, atomItemPatterns: _*)

}
