package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.results.NoResult
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.github.propi.rdfrules.gui.{ActionProgress, Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.concurrent.Future

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class ExportRules(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.ExportRules
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, true, "path", "Path", validator = NonEmpty, "file"),
    new Select("format", "Rules format", Constants("ndjson" -> "NDJSON (as model - parsable and streaming)", "json" -> "JSON (as model - parsable)", "txt" -> "Text (unparsable)", "cache" -> "Cache (internal binary format)"), Some("ndjson"))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def buildActionProgress(id: Future[String]): Option[ActionProgress] = Some(new NoResult(info.title, id))
}