package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.{ChooseFileFromWorkspace, OptionalText, Select, Text}
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class LoadDataset(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, "path"),
    new OptionalText[String]("url", "URL"),
    new Select("format", "RDF format", Constants("ttl" -> "Turtle", "nt" -> "N-Triples", "nq" -> "N-Quads", "xml" -> "RDF/XML", "json" -> "JSON-LD", "trig" -> "TriG", "trix" -> "TriX", "tsv" -> "TSV", "sql" -> "SQL", "cache" -> "Cache"))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  override def validate(): Boolean = {
    if (super.validate()) {
      val isValid = Iterator(properties.value.headOption, properties.value.lift(1)).flatten.collect {
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