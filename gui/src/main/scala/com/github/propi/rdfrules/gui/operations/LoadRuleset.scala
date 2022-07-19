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
    new ChooseFileFromWorkspace(Workspace.loadFiles, "path", validator = NonEmpty),
    new Select("format", "Rules format", Constants("cache" -> "Ruleset cache", "json" -> "JSON", "ndjson" -> "NDJSON")),
    new FixedText[Int]("parallelism", "Parallelism", "0", GreaterThanOrEqualsTo[Int](0))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}