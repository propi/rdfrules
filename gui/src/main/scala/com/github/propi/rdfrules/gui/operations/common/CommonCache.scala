package com.github.propi.rdfrules.gui.operations.common

import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.github.propi.rdfrules.gui.properties.{Checkbox, DynamicElement, FixedText}
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.thoughtworks.binding.Binding.{Constants, Var}

import java.util.UUID

abstract class CommonCache(fromOperation: Operation, val info: OperationInfo, id: Option[String]) extends Operation {
  val properties: Constants[Property] = {
    val path = new DynamicElement(Constants(
      context.use("On-disk")(implicit context => new FixedText[String]("path", "Path", validator = NonEmpty)),
      context.use("In-memory")(implicit context => new FixedText[String]("path", "Cache ID", validator = NonEmpty, default = id.getOrElse(UUID.randomUUID().toString)))
    ))
    path.setElement(1)
    Constants(
      new Checkbox("inMemory", "In-memory", true, {
        case true => path.setElement(1)
        case false => path.setElement(0)
      }),
      path,
      new Checkbox("revalidate", "Revalidate")
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}