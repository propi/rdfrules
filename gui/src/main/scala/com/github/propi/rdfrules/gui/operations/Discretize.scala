package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.Property.SummaryTitle
import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators._
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Discretize(fromOperation: Operation, val info: OperationInfo) extends Operation {

  val properties: Constants[Property] = {
    Constants(
      new OptionalText[String]("subject", "Subject", validator = RegExp("<.*>|\\w+:.*", true)),
      new OptionalText[String]("predicate", "Predicate", validator = RegExp("<.*>|\\w+:.*", true)),
      new OptionalText[String]("object", "Object", validator = RegExp("[><]=? \\d+(\\.\\d+)?|[\\[\\(]\\d+(\\.\\d+)?;\\d+(\\.\\d+)?[\\]\\)]", true)),
      new OptionalText[String]("graph", "Graph", validator = RegExp("<.*>|\\w+:.*", true)),
      new Checkbox("inverse", "Negation"),
      Group("task", "Strategy", "strategy") { implicit context =>
        val bins = context.use("Equidistance or Equifrequency")(implicit context => new DynamicElement(Constants(new FixedText[Int]("bins", "Number of bins", validator = GreaterThan[Int](0))), true))
        val support = context.use("Equisize")(implicit context => new DynamicElement(Constants(new FixedText[Double]("support", "Min support", validator = GreaterThanOrEqualsTo(0.0).map[String] & LowerThanOrEqualsTo(1.0).map[String])), true))

        def activeStrategy(hasBins: Boolean, hasSupport: Boolean): Unit = {
          if (hasBins) bins.setElement(0) else bins.setElement(-1)
          if (hasSupport) support.setElement(0) else support.setElement(-1)
        }

        Constants(
          new Select("name", "Name",
            Constants("EquidistanceDiscretizationTask" -> "Equidistance", "EquifrequencyDiscretizationTask" -> "Equifrequency", "EquisizeDiscretizationTask" -> "Equisize"),
            Some("EquidistanceDiscretizationTask"),
            {
              case ("EquisizeDiscretizationTask", _) => activeStrategy(false, true)
              case ("EquidistanceDiscretizationTask", _) | ("EquifrequencyDiscretizationTask", _) => activeStrategy(true, false)
              case _ => activeStrategy(false, false)
            },
            SummaryTitle.NoTitle,
          ),
          bins,
          support
        )
      }
    )
  }

  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}