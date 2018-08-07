package com.github.propi.rdfrules.java

import com.github.propi.rdfrules.data.{TripleItem => ScalaTripleItem}
import com.github.propi.rdfrules.java.data.TripleItem
import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.ruleset.ResolvedRule

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 13. 5. 2018.
  */
object ScalaResolvedAtom {

  class ItemWrapper(val item: ResolvedRule.Atom.Item)

  class ItemVariableWrapper(override val item: ResolvedRule.Atom.Item.Variable) extends ItemWrapper(item)

  class ItemConstantWrapper(override val item: ResolvedRule.Atom.Item.Constant) extends ItemWrapper(item)

  def variable(x: String): ItemVariableWrapper = new ItemVariableWrapper(ResolvedRule.Atom.Item(x).asInstanceOf[ResolvedRule.Atom.Item.Variable])

  def variable(x: Char): ItemVariableWrapper = new ItemVariableWrapper(ResolvedRule.Atom.Item(x).asInstanceOf[ResolvedRule.Atom.Item.Variable])

  def constant(x: TripleItem): ItemConstantWrapper = new ItemConstantWrapper(ResolvedRule.Atom.Item(x.asScala()).asInstanceOf[ResolvedRule.Atom.Item.Constant])

  def graphs(atom: ResolvedRule.Atom): java.util.Set[TripleItem.Uri] = atom match {
    case x: ResolvedRule.Atom.GraphBased => x.graphs.map(TripleItemConverters.toJavaUri).asJava
    case _ => Set.empty[TripleItem.Uri].asJava
  }

  def resolvedAtom(subject: ResolvedRule.Atom.Item, predicate: ScalaTripleItem.Uri, `object`: ResolvedRule.Atom.Item): ResolvedRule.Atom = ResolvedRule.Atom(subject, predicate, `object`)

}