package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class MapQuads(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.MapQuads
  val properties: Constants[Property] = Constants(
    Group("search", "Search", Constants(
      OptionalText("subject", "Subject"),
      OptionalText("predicate", "Predicate"),
      OptionalText("object", "Object"),
      OptionalText("graph", "Graph"),
      Checkbox("inverse", "Inverse")
    )),
    Group("replacement", "Replacement", Constants(
      OptionalText("subject", "Subject"),
      OptionalText("predicate", "Predicate"),
      OptionalText("object", "Object"),
      OptionalText("graph", "Graph")
    ))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}