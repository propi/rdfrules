package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui._
import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.results.NoResult
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class ExportPrediction(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.ExportPrediction
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, true, "path", "Path", validator = NonEmpty, "file"),
    new Select("format", "Prediction format", Constants("ndjson" -> "NDJSON", "cache" -> "Cache (internal binary format)"))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new NoResult(info.title, id))
}