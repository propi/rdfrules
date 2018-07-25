package com.github.propi.rdfrules.data

import java.io.File

import com.github.propi.rdfrules.data.Quad.QuadTraversableView
import com.github.propi.rdfrules.data.RdfSource.Tsv
import com.github.propi.rdfrules.utils.OutputStreamBuilder
import com.github.propi.rdfrules.data.formats.Tsv._
import com.github.propi.rdfrules.data.formats.JenaLang._
import org.apache.jena.riot.RDFFormat

/**
  * Created by Vaclav Zeman on 4. 10. 2017.
  */
trait RdfWriter[+T <: RdfSource] {
  def writeToOutputStream(quads: QuadTraversableView, outputStreamBuilder: OutputStreamBuilder): Unit

  def writeToOutputStream(dataset: Dataset, outputStreamBuilder: OutputStreamBuilder): Unit = writeToOutputStream(dataset.quads, outputStreamBuilder)

  def writeToOutputStream(graph: Graph, outputStreamBuilder: OutputStreamBuilder): Unit = writeToOutputStream(Dataset(graph), outputStreamBuilder)
}

object RdfWriter {

  implicit object NoWriter extends RdfWriter[Nothing] {
    def writeToOutputStream(quads: QuadTraversableView, outputStreamBuilder: OutputStreamBuilder): Unit = throw new IllegalStateException("No specified RdfWriter.")
  }

  def apply(file: File): RdfWriter[RdfSource] = file.getName.replaceAll(".*\\.", "").toLowerCase match {
    case "nt" => RDFFormat.NTRIPLES_UTF8
    case "nq" => RDFFormat.NQUADS_UTF8
    case "ttl" => RDFFormat.TURTLE_FLAT
    case "trig" => RDFFormat.TRIG_BLOCKS
    case "trix" => RDFFormat.TRIX
    case "tsv" => Tsv
    case x => throw new IllegalArgumentException(s"Unsupported RDF format for streaming writing: $x")
  }

}