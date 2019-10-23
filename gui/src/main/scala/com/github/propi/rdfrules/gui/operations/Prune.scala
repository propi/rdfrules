package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Prune(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new Checkbox("onlyFunctionalProperties", "Only functional properties", true, "Generate only functional properties. That means only one object can be predicted for pair (subject, predicate).")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}