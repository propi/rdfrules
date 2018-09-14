package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class FilterQuads(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.FilterQuads
  val properties: Constants[Property] = Constants(
    DynamicGroup("or", "Filter by (logical OR)", () => Constants(
      OptionalText[String]("subject", "Subject"),
      OptionalText[String]("predicate", "Predicate"),
      OptionalText[String]("object", "Object"),
      OptionalText[String]("graph", "Graph"),
      Checkbox("inverse", "Inverse")
    ))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}