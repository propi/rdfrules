package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.{ChooseFileFromWorkspace, FixedText, Select}
import com.github.propi.rdfrules.gui.utils.CommonValidators.{GreaterThanOrEqualsTo, NonEmpty}
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.github.propi.rdfrules.gui.utils.StringConverters._

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class LoadRuleset(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, "path", description = "You can load a serialized ruleset file from the workspace on the server side (just click onto a file name).", validator = NonEmpty),
    new Select("format", "Rules format", Constants("cache" -> "Ruleset cache", "json" -> "JSON", "ndjson" -> "NDJSON"), description = "The ruleset format. Default is \"Ruleset cache\"."),
    new FixedText[Int]("parallelism", "Parallelism", "0", "If the value is lower than or equal to 0 and greater than 'all available cores' then the parallelism level is set to 'all available cores'.", GreaterThanOrEqualsTo[Int](0))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}
