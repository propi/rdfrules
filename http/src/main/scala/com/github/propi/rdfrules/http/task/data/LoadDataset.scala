package com.github.propi.rdfrules.http.task.data

import java.net.URL

import com.github.propi.rdfrules.data.{Dataset, RdfReader, RdfSource}
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class LoadDataset(path: Option[String], url: Option[URL], format: Option[Option[RdfSource]]) extends Task.NoInputDatasetTask {
  val companion: TaskDefinition = LoadDataset

  def execute(input: Task.NoInput.type): Dataset = format match {
    case Some(Some(x)) =>
      implicit val reader: RdfReader = x match {
        case x: RdfSource.Tsv.type => x
        case x: RdfSource.JenaLang => x.lang
      }
      (path, url) match {
        case (_, Some(url)) => Dataset(url.openStream())
        case (Some(path), _) => Dataset(Workspace.path(path))
        case _ => throw ValidationException("NoSource", "No path or url is specified.")
      }
    case Some(None) => (path, url) match {
      case (_, Some(url)) => Dataset.fromCache(url.openStream())
      case (Some(path), _) => Dataset.fromCache(Workspace.path(path))
      case _ => throw ValidationException("NoSource", "No path or url is specified.")
    }
    case None => path match {
      case Some(path) => Dataset(Workspace.path(path))
      case None => throw ValidationException("NoRdfFormat", "For URL you must specify an RDF format.")
    }
  }
}

object LoadDataset extends TaskDefinition {
  val name: String = "LoadDataset"
}