package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.{ChooseFileFromWorkspace, Select}
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class LoadModel(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, "path", description = "You can load a serialized model file (json or cache format) from the workspace on the server side (just click onto a file name).", validator = NonEmpty),
    new Select("format", "Rules format", Constants("json" -> "JSON", "ndjson" -> "NDJSON", "cache" -> "Model cache"), description = "Rules format."),
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}