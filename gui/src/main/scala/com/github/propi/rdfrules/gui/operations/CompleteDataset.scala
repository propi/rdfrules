package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui._
import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class CompleteDataset(fromOperation: Operation, val info: OperationInfo, allowToChooseModel: Boolean) extends Operation {
  val properties: Constants[Property] = {
    Constants(
      List(
        if (allowToChooseModel) Some(new ChooseFileFromWorkspace(Workspace.loadFiles, "path", description = "You can load a serialized model file from the workspace on the server side (just click onto a file name).", validator = NonEmpty)) else None,
        if (allowToChooseModel) Some(new Select("format", "Rules format", Constants("json" -> "JSON", "ndjson" -> "NDJSON", "cache" -> "Model cache"), description = "Rules format.")) else None,
        Some(new Checkbox("onlyFunctionalProperties", "Only functional properties", true, "Generate only functional properties. That means only one object can be predicted for pair (subject, predicate).")),
        Some(new Checkbox("onlyNewPredicates", "Only new properties", true, "If checked, the input dataset is completed by such predicted triple which are missing and the subject did not contain any information related with the predicted predicate (it is new valuable knowledge)."))
      ).flatten: _*
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}