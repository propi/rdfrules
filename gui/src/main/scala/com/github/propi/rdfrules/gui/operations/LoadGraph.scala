package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.{ChooseFileFromWorkspace, OptionalText, Select}
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class LoadGraph(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.LoadGraph
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, "path", description = "It is possible to load a file from the workspace on the server side (just click onto a file name), or you can load any remote file from URL (see below)."),
    new OptionalText[String]("url", "URL", description = "A URL to a remote file to be loaded. If this is specified then the workspace file is ommited."),
    new Select("format", "RDF format", Constants("ttl" -> "Turtle", "nt" -> "N-Triples", "nq" -> "N-Quads", "xml" -> "RDF/XML", "json" -> "JSON-LD", "trig" -> "TriG", "trix" -> "TriX", "tsv" -> "TSV", "cache" -> "Cache"), description = "The RDF format is automatically detected from the file extension. But, you can specify the format explicitly."),
    new OptionalText[String]("graphName", "Graph name", description = "Name for this loaded graph. It must have the URI notation in angle brackets, e.g., <dbpedia> or <http://dbpedia.org>.", validator = RegExp("<.*>", true))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}
