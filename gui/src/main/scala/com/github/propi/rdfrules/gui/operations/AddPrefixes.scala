package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class AddPrefixes(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.AddPrefixes
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, "path", description = "It is possible to load a file with prefixes in the Turtle (.ttl) format from the workspace on the server side (just click onto a file name), or you can load any remote prefix file from URL (see below)."),
    new OptionalText[String]("url", "URL", description = "A URL to a remote file with prefixes in the Turtle (.ttl) format to be loaded. If this is specified then the workspace file is ommited."),
    new DynamicGroup("prefixes", "Hand-defined prefixes", () => Constants(
      new OptionalText[String]("prefix", "Prefix", description = "A short name for the namespace.", validator = RegExp("[0-9a-zA-Z]\\w*")),
      new OptionalText[String]("nameSpace", "Namespace", description = "A namespace URI to be shortened. It should end with the slash or hash symbol, e.g., http://dbpedia.org/property/.", validator = RegExp("\\S+[/#?=&]"))
    ), "Here, you can define your own prefixes manually.")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}