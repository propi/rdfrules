package com.github.propi.rdfrules.java

/**
  * Created by Vaclav Zeman on 13. 5. 2018.
  */
object ScalaResolvedAtom {

  class ItemWrapper(val item: com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item)

  class ItemVariableWrapper(override val item: com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item.Variable) extends ItemWrapper(item)

  class ItemConstantWrapper(override val item: com.github.propi.rdfrules.ruleset.ResolvedRule.Atom.Item.Constant) extends ItemWrapper(item)

}
