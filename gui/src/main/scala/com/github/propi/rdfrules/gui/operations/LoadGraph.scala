package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.{ChooseFileFromWorkspace, OptionalText, Select}
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class LoadGraph(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.LoadGraph
  val properties: Constants[Property] = Constants(
    new ChooseFileFromWorkspace(Workspace.loadFiles, "path"),
    new OptionalText[String]("url", "URL"),
    new Select("format", "RDF format", Constants("ttl" -> "Turtle", "nt" -> "N-Triples", "nq" -> "N-Quads", "xml" -> "RDF/XML", "json" -> "JSON-LD", "trig" -> "TriG", "trix" -> "TriX", "tsv" -> "TSV", "cache" -> "Cache")),
    new OptionalText[String]("graphName", "Graph name")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}
