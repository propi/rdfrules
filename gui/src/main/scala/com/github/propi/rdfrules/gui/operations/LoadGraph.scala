package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.Workspace.FileValue
import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.thoughtworks.binding.Binding.BindingInstances.monadSyntax._

import scala.language.reflectiveCalls

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class LoadGraph(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    val validator = RegExp(".+[.](ttl|nt|nq|json|jsonld|xml|rdf|owl|trig|trix|tsv|sql|cache)([.](gz|bz2))?$", true)
    val fileChooser = new ChooseFileFromWorkspace(Workspace.loadFiles, false, "path", validator = validator, summaryTitle = "file")
    val url = new OptionalText[String]("url", "URL", summaryTitle = "url", validator = validator)

    def settingsSelector(x: Option[FileValue.File], y: String): Int = {
      if (x.map(_.path).orElse(Some(y)).exists(_.matches(".+[.]tsv([.](gz|bz2))?"))) 0 else -1
    }

    val settings = new DynamicElementBinding(
      Constants(new Select("settings", "TSV parser", Constants("tsvRaw" -> "Raw", "tsvParsedUris" -> "Parsed URIs", "tsvParsedLiterals" -> "Parsed URIs and literals"), Some("tsvParsedUris"))),
      fileChooser.getSelectedFileBinding -> url.getTextBinding,
      true
    )(x => settingsSelector(x._1.value, x._2.value), x => x._1.tuple(x._2).map(x => settingsSelector(x._1, x._2)))
    Constants(
      fileChooser,
      url,
      settings,
      //new Select("format", "RDF format", Constants("ttl" -> "Turtle", "nt" -> "N-Triples", "nq" -> "N-Quads", "xml" -> "RDF/XML", "json" -> "JSON-LD", "trig" -> "TriG", "trix" -> "TriX", "tsv" -> "TSV", "sql" -> "SQL", "cache" -> "Cache")),
      new OptionalText[String]("graphName", "Graph name", validator = RegExp("<.*>", true), summaryTitle = "name")
    )
  }
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
