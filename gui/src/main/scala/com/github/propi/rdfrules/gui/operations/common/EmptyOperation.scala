package com.github.propi.rdfrules.gui.operations.common

import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

abstract class EmptyOperation(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants()
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}