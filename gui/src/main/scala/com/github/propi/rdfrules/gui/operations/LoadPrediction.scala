package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.{ChooseFileFromWorkspace, Select}
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class LoadPrediction(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, false, "path", validator = NonEmpty, summaryTitle = "file"),
    new Select("format", "Rules format", Constants("ndjson" -> "NDJSON", "json" -> "JSON", "cache" -> "Cache (internal binary format)"), Some("ndjson"))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}