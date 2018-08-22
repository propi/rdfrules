package com.github.propi.rdfrules.http.task.data

import java.net.URL

import com.github.propi.rdfrules.data.{Dataset, Prefix}
import com.github.propi.rdfrules.http.Workspace
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class AddPrefixes(path: Option[String], url: Option[URL], prefixes: Seq[Prefix]) extends Task[Dataset, Dataset] {
  val companion: TaskDefinition = AddPrefixes

  def execute(input: Dataset): Dataset = {
    val x = (path, url) match {
      case (_, Some(url)) => input.addPrefixes(url.openStream())
      case (Some(path), _) => input.addPrefixes(Workspace.path(path))
      case _ => input
    }
    x.addPrefixes(prefixes)
  }
}

object AddPrefixes extends TaskDefinition {
  val name: String = "AddPrefixes"
}