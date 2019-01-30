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
    new DynamicGroup("or", "Filter by (logical OR)", () => Constants(
      new OptionalText[String]("subject", "Subject"),
      new OptionalText[String]("predicate", "Predicate"),
      new OptionalText[String]("object", "Object"),
      new OptionalText[String]("graph", "Graph"),
      new Checkbox("inverse", "Negation")
    ))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}