package com.github.propi.rdfrules.gui.operations.common

import com.github.propi.rdfrules.gui.properties.{Checkbox, ChooseFileFromWorkspace, DynamicElement, FixedText}
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

import java.util.UUID

abstract class CommonCache(fromOperation: Operation, val info: OperationInfo, id: Option[String]) extends Operation {
  private val onDiskPath = context.use("On-disk")(implicit context => new ChooseFileFromWorkspace(Workspace.loadFiles, true, "path", "Path", validator = NonEmpty, "file"))
  private val inMemoryId = context.use("In-memory")(implicit context => new FixedText[String]("path", "Cache ID", validator = NonEmpty, default = id.getOrElse(UUID.randomUUID().toString)))
  private val path = new DynamicElement(Constants(
    onDiskPath,
    inMemoryId
  ))
  private val inMemoryProperty = new Checkbox("inMemory", "In-memory", true, {
    case true => path.setElement(1)
    case false => path.setElement(0)
  }, "in-memory")
  private val revalidateProperty = new Checkbox("revalidate", "Revalidate")
  private var lastRevalidatedId = ""

  val properties: Constants[Property] = {
    path.setElement(1)
    Constants(
      inMemoryProperty,
      path,
      revalidateProperty
    )
  }

  def getId: Option[String] = if (inMemoryProperty.isChecked) Some(inMemoryId.getText) else None

  def revalidated[T](f: CommonCache => T): T = {
    val revalidatedId = getId.map(x => s"inmemory:$x").getOrElse(s"ondisk:${onDiskPath.getSelectedFile.map(_.path).getOrElse("")}")
    if (revalidatedId != lastRevalidatedId) {
      //auto revalidation is disabled for the first start of a selected cache
      lastRevalidatedId = revalidatedId
      f(this)
    } else {
      val lastState = revalidateProperty.isChecked
      try {
        revalidateProperty.setValue(true)
        f(this)
      } finally {
        revalidateProperty.setValue(lastState)
      }
    }
  }

  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}