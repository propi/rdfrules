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
    new Group("search", "Search", Constants(
      new OptionalText[String]("subject", "Subject"),
      new OptionalText[String]("predicate", "Predicate"),
      new OptionalText[String]("object", "Object"),
      new OptionalText[String]("graph", "Graph"),
      new Checkbox("inverse", "Negation")
    )),
    new Group("replacement", "Replacement", Constants(
      new OptionalText[String]("subject", "Subject"),
      new OptionalText[String]("predicate", "Predicate"),
      new OptionalText[String]("object", "Object"),
      new OptionalText[String]("graph", "Graph")
    ))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}