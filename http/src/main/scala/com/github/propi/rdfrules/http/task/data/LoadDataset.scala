package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.{Dataset, RdfReader, RdfSource}
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import com.github.propi.rdfrules.http.util.BasicExceptions.ValidationException
import com.github.propi.rdfrules.utils.Debugger

import java.net.URL

/**
  * Created by Vaclav Zeman on 7. 8. 2018.
  */
class LoadDataset(path: Option[String], url: Option[URL])(implicit debugger: Debugger, sourceSettings: RdfSource.Settings) extends Task.NoInputDatasetTask {
  val companion: TaskDefinition = LoadDataset

  def execute(input: Task.NoInput.type): Dataset = {
    val dataset = path match {
      case Some(path) => Dataset(Workspace.path(path))
      case None => url match {
        case Some(url) => Dataset(url.openStream())(RdfReader(url.toString))
        case None => throw ValidationException("NoRdfFormat", "For URL you must specify an RDF format extension, e.g. ?extension=.nt")
      }
    }
    dataset.withDebugger()
  }
}

object LoadDataset extends TaskDefinition {
  val name: String = "LoadDataset"
}