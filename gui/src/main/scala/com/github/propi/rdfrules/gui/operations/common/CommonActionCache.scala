package com.github.propi.rdfrules.gui.operations.common

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.results.NoResult
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{ActionProgress, Operation, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

abstract class CommonActionCache(fromOperation: Operation) extends Operation {
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, "path", "Path", validator = NonEmpty, "path"),
    new Hidden[Boolean]("inMemory", "false"),
    new Hidden[Boolean]("revalidate", "true")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new NoResult(info.title, id))
}