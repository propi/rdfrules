package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.{ChooseFileFromWorkspace, OptionalText, Select}
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property, Workspace}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class LoadDataset(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.LoadDataset
  val properties: Constants[Property] = Constants(
    ChooseFileFromWorkspace(Workspace.loadFiles, "path"),
    OptionalText("url", "URL"),
    Select("format", "RDF format", Constants("ttl" -> "Turtle", "nt" -> "N-Triples", "nq" -> "N-Quads", "xml" -> "RDF/XML", "json" -> "JSON-LD", "trig" -> "TriG", "trix" -> "TriX", "tsv" -> "TSV", "cache" -> "Cache"))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}