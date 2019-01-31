package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.{ChooseFileFromWorkspace, OptionalText, Select, Text}
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class LoadDataset(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.LoadDataset
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, "path", description = "It is possible to load a file from the workspace on the server side (just click onto a file name), or you can load any remote file from URL (see below)."),
    new OptionalText[String]("url", "URL", description = "A URL to a remote file to be loaded. If this is specified then the workspace file is ommited."),
    new Select("format", "RDF format", Constants("ttl" -> "Turtle", "nt" -> "N-Triples", "nq" -> "N-Quads", "xml" -> "RDF/XML", "json" -> "JSON-LD", "trig" -> "TriG", "trix" -> "TriX", "tsv" -> "TSV", "cache" -> "Cache"), description = "The RDF format is automatically detected from the file extension. But, you can specify the format explicitly.")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def validate(): Boolean = {
    if (super.validate()) {
      val isValid = Iterator(properties.value.lift(0), properties.value.lift(1)).flatten.collect {
        case x: ChooseFileFromWorkspace => x.getSelectedFile.map(_.path).getOrElse("")
        case x: Text => x.getText
      }.exists(_.nonEmpty)
      if (!isValid) {
        errorMsg.value = Some("No source is selected.")
      }
      isValid
    } else {
      false
    }
  }
}