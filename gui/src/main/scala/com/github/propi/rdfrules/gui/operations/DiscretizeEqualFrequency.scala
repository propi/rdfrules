package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.github.propi.rdfrules.gui.utils.StringConverters._

import scala.scalajs.js
import scala.scalajs.js.Dictionary

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class DiscretizeEqualFrequency(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.DiscretizeEqualFrequency
  val properties: Constants[Property] = Constants(
    OptionalText[String]("subject", "Subject"),
    OptionalText[String]("predicate", "Predicate"),
    OptionalText[String]("object", "Object"),
    OptionalText[String]("graph", "Graph"),
    Checkbox("inverse", "Inverse"),
    FixedText[Int]("bins", "Number of bins")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override protected def propertiesToJson: Dictionary[js.Any] = {
    val x = super.propertiesToJson
    val bins = x.remove("bins").get
    x += ("task" -> js.Dynamic.literal(name = "EquifrequencyDiscretizationTask", bins = bins))
    x
  }
}