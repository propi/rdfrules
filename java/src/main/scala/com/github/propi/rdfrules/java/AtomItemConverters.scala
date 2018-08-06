package com.github.propi.rdfrules.java

import com.github.propi.rdfrules.java.rule.RulePattern.AtomItemPattern
import com.github.propi.rdfrules.java.rule.{Atom, ResolvedAtom, RulePattern}
import com.github.propi.rdfrules.rule.AtomPattern
import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 13. 5. 2018.
  */
object AtomItemConverters {

  def toJavaAtomItem(item: com.github.propi.rdfrules.rule.Atom.Item): Atom.Item = item match {
    case x: com.github.propi.rdfrules.rule.Atom.Variable => new Atom.Variable(x)
    case x: com.github.propi.rdfrules.rule.Atom.Constant => new Atom.Constant(x)
  }

  def toJavaResolvedAtomItem(item: com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item): ResolvedAtom.Item = item match {
    case x: com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item.Variable => new ResolvedAtom.Variable(new ScalaResolvedAtom.ItemVariableWrapper(x))
    case x: com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item.Constant => new ResolvedAtom.Constant(new ScalaResolvedAtom.ItemConstantWrapper(x))
  }

  def toScalaAtomItemPattern(itemPattern: AtomItemPattern): AtomPattern.AtomItemPattern = itemPattern match {
    case _: RulePattern.Any => AtomPattern.AtomItemPattern.Any
    case _: RulePattern.AnyConstant => AtomPattern.AtomItemPattern.AnyConstant
    case _: RulePattern.AnyVariable => AtomPattern.AtomItemPattern.AnyVariable
    case x: RulePattern.Constant => AtomPattern.AtomItemPattern.Constant(x.getConstant.asScala())
    case x: RulePattern.Variable => AtomPattern.AtomItemPattern.Variable(com.github.propi.rdfrules.rule.Atom.Variable(x.getVariable))
    case x: RulePattern.OneOf => AtomPattern.AtomItemPattern.OneOf(x.getCol.asScala.toSeq.map(toScalaAtomItemPattern))
    case x: RulePattern.NoneOf => AtomPattern.AtomItemPattern.NoneOf(x.getCol.asScala.toSeq.map(toScalaAtomItemPattern))
    case _ => AtomPattern.AtomItemPattern.Any
  }

}