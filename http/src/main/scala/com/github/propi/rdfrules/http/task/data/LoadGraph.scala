package com.github.propi.rdfrules.http.task.data

import java.net.URL

import com.github.propi.rdfrules.data.{Dataset, Graph, RdfReader, RdfSource, TripleItem}
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class LoadGraph(graphName: Option[TripleItem.Uri], path: Option[String], url: Option[URL], format: Option[Option[RdfSource]]) extends Task.NoInputDatasetTask {
  val companion: TaskDefinition = LoadGraph

  def execute(input: Task.NoInput.type): Dataset = format match {
    case Some(Some(x)) =>
      implicit val reader: RdfReader = x match {
        case x: RdfSource.Tsv.type => x
        case x: RdfSource.Sql.type => x
        case x: RdfSource.JenaLang => x.lang
      }
      (path, url) match {
        case (_, Some(url)) => graphName.map(x => Graph(x, url.openStream())).getOrElse(Graph(url.openStream())).toDataset
        case (Some(path), _) => graphName.map(x => Graph(x, Workspace.path(path))).getOrElse(Graph(Workspace.path(path))).toDataset
        case _ => throw ValidationException("NoSource", "No path or url is specified.")
      }
    case Some(None) => (path, url) match {
      case (_, Some(url)) => graphName.map(x => Graph.fromCache(x, url.openStream())).getOrElse(Graph.fromCache(url.openStream())).toDataset
      case (Some(path), _) => graphName.map(x => Graph.fromCache(x, Workspace.path(path))).getOrElse(Graph.fromCache(Workspace.path(path))).toDataset
      case _ => throw ValidationException("NoSource", "No path or url is specified.")
    }
    case None => path match {
      case Some(path) => graphName.map(x => Graph(x, Workspace.path(path))).getOrElse(Graph(Workspace.path(path))).toDataset
      case None => throw ValidationException("NoRdfFormat", "For URL you must specify an RDF format.")
    }
  }
}

object LoadGraph extends TaskDefinition {
  val name: String = "LoadGraph"
}