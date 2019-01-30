package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class ComputeLift(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.ComputeLift
  val properties: Constants[Property] = Constants(
    new OptionalText[Double]("min", "Min confidence", "0.5", "A minimal cofidence threshold. This operation first counts the standard confidence for all rules and filter them by this minimal threshold, then it counts the lift measure by the computed cofindence. The value range is between 0.01 and 1 included. Default value is set to 0.5.", RegExp("1(\\.0+)?|0\\.0[1-9]\\d*|0\\.[1-9]\\d*", true))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}