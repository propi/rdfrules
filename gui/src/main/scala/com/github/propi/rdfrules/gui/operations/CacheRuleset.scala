package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class CacheRuleset(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new FixedText[String]("path", "Path", description = "A relative path to a file related to the workspace where the serialized ruleset should be saved.", validator = NonEmpty)
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}