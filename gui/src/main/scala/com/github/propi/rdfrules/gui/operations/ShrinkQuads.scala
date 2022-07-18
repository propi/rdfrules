package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.GreaterThanOrEqualsTo
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.github.propi.rdfrules.gui.utils.StringConverters._

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class ShrinkQuads(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new FixedText[Int]("start", "From", validator = GreaterThanOrEqualsTo[Int](0)),
    new FixedText[Int]("end", "Until", validator = GreaterThanOrEqualsTo[Int](0))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}