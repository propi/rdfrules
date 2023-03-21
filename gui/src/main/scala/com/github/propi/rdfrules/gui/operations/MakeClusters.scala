package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.{GreaterThanOrEqualsTo, LowerThanOrEqualsTo}
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class MakeClusters(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new OptionalText[Int]("minNeighbours", "Min neighbours", default = "2", validator = GreaterThanOrEqualsTo[Int](1)),
    new OptionalText[Double]("minSimilarity", "Min similarity", default = "0.85", validator = GreaterThanOrEqualsTo(0.0).map[String] & LowerThanOrEqualsTo(1.0).map[String])
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}