package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui._
import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class PredictTriples(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, "path", description = "You can load a serialized model file from the workspace on the server side (just click onto a file name).", validator = NonEmpty),
    new Select("format", "Rules format", Constants("json" -> "JSON", "cache" -> "Model cache"), description = "Rules format."),
    new Checkbox("onlyFunctionalProperties", "Only function properties", true, "Generate only functional properties. That means only one object can be predicted for pair (subject, predicate).")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}