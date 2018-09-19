package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class ComputeLift(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.ComputeLift
  val properties: Constants[Property] = Constants(
    new OptionalText[Double]("min", "Min confidence")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}