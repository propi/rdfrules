package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.results.NoResult
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.github.propi.rdfrules.gui.{ActionProgress, Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class ExportRules(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.ExportRules
  val properties: Constants[Property] = Constants(
    new FixedText[String]("path", "Path", description = "A relative path to a file related to the workspace where the exported rules should be saved.", validator = NonEmpty),
    new Select("format", "Rules format", Constants("txt" -> "Text", "json" -> "JSON"), description = "The output format is automatically detected from the file extension (.txt or .json). But, you can specify the format explicitly.")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new NoResult(info.title, id))
}