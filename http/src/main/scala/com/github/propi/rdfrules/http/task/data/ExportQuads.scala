package com.github.propi.rdfrules.http.task.data

import com.github.propi.rdfrules.data.{Dataset, RdfSource, RdfWriter}
import com.github.propi.rdfrules.http.task.{Task, TaskDefinition}
import org.apache.jena.riot.RDFFormat

/**
  * Created by Vaclav Zeman on 9. 8. 2018.
  */
class ExportQuads(path: String, format: Option[Either[RdfSource.Tsv.type, RDFFormat]]) extends Task[Dataset, Unit] {
  val companion: TaskDefinition = ExportQuads

  def execute(input: Dataset): Unit = format match {
    case Some(x) =>
      implicit val writer: RdfWriter = x.fold(x => x, x => x)
      input.export(path)
    case None => input.export(path)
  }
}

object ExportQuads extends TaskDefinition {
  val name: String = "ExportQuads"
}