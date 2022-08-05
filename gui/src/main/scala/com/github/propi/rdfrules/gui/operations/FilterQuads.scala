package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class FilterQuads(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    DynamicGroup("or", "Filter by (logical OR)", "by") { implicit context =>
      Constants(
        new OptionalText[String]("subject", "Subject", validator = RegExp("<.*>|.*:.*", true), summaryTitle = "subject"),
        new OptionalText[String]("predicate", "Predicate", validator = RegExp("<.*>|.*:.*", true), summaryTitle = "predicate"),
        new OptionalText[String]("object", "Object", summaryTitle = "object"),
        new OptionalText[String]("graph", "Graph", validator = RegExp("<.*>|.*:.*", true), summaryTitle = "graph"),
        new Checkbox("inverse", "Negation", summaryTitle = "negated")
      )
    }
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}