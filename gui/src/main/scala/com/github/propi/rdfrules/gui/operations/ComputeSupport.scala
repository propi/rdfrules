package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.{GreaterThanOrEqualsTo, RegExp}
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class ComputeSupport(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    val min = new DynamicElement(Constants(
      context.use("Support")(implicit context => new FixedText[Int]("min", "Min support", "", GreaterThanOrEqualsTo[Int](1), "min")),
      context.use("Head coverage")(implicit context => new FixedText[Double]("min", "Min head coverage", "", RegExp("1(\\.0+)?|0\\.00[1-9]\\d*|0\\.0?[1-9]\\d*"), "min")),
    ), true)

    def activeStrategy(minIndex: Int): Unit = {
      min.setElement(minIndex)
    }

    Constants(
      new Select("name", "Name",
        Constants("Support" -> "Support", "HeadCoverage" -> "Head coverage"),
        Some("Support"),
        {
          case ("HeadCoverage", _) => activeStrategy(1)
          case _ => activeStrategy(0)
        },
        Property.SummaryTitle.NoTitle
      ),
      min
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}