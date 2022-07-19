package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.ChooseFileFromWorkspace
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class LoadPrediction(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, "path", validator = NonEmpty)
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}