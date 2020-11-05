package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.results.NoResult
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.github.propi.rdfrules.gui.{ActionProgress, Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.github.propi.rdfrules.gui.utils.StringConverters._

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class CacheDataset(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.CacheDatasetAction
  val properties: Constants[Property] = Constants(
    new FixedText[String]("path", "Path", description = "A relative path to a file related to the workspace where the serialized dataset should be saved.", validator = NonEmpty),
    new Hidden[Boolean]("inMemory", "false"),
    new Hidden[Boolean]("revalidate", "true")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new NoResult(info.title, id))
}