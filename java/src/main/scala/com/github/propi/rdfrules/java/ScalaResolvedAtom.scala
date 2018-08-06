package com.github.propi.rdfrules.java

import com.github.propi.rdfrules.java.data.TripleItem

/**
  * Created by Vaclav Zeman on 13. 5. 2018.
  */
object ScalaResolvedAtom {

  class ItemWrapper(val item: com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item)

  class ItemVariableWrapper(override val item: com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item.Variable) extends ItemWrapper(item)

  class ItemConstantWrapper(override val item: com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item.Constant) extends ItemWrapper(item)

  def variable(x: String): ItemVariableWrapper = new ItemVariableWrapper(com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item(x).asInstanceOf[com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item.Variable])

  def variable(x: Char): ItemVariableWrapper = new ItemVariableWrapper(com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item(x).asInstanceOf[com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item.Variable])

  def constant(x: TripleItem): ItemConstantWrapper = new ItemConstantWrapper(com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item(x.asScala()).asInstanceOf[com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item.Constant])

}
