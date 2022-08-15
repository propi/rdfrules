package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class AddPrefixes(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, false, "path"),
    new OptionalText[String]("url", "URL"),
    DynamicGroup("prefixes", "Hand-defined prefixes") { implicit context =>
      Constants(
        new OptionalText[String]("prefix", "Prefix", validator = RegExp("[0-9a-zA-Z]\\w*")),
        new OptionalText[String]("nameSpace", "Namespace", validator = RegExp("\\S+[/#?=&]"))
      )
    }
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}