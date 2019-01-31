package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.ChooseFileFromWorkspace
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class LoadRuleset(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.LoadRuleset
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, "path", description = "You can load a serialized ruleset file from the workspace on the server side (just click onto a file name).", validator = NonEmpty)
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}
