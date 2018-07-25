package com.github.propi.rdfrules.data

import java.io.{File, FileInputStream}

import com.github.propi.rdfrules.data.Quad.QuadTraversableView
import com.github.propi.rdfrules.data.RdfSource.Tsv
import com.github.propi.rdfrules.data.formats.JenaLang._
import com.github.propi.rdfrules.data.formats.Tsv._
import com.github.propi.rdfrules.utils.InputStreamBuilder
import org.apache.jena.riot.Lang

/**
  * Created by Vaclav Zeman on 27. 6. 2017.
  */
trait RdfReader[+T <: RdfSource] {
  def fromInputStream(inputStreamBuilder: InputStreamBuilder): QuadTraversableView

  def fromFile(file: File): QuadTraversableView = fromInputStream(new FileInputStream(file))
}

object RdfReader {

  implicit object NoReader extends RdfReader[Nothing] {
    def fromInputStream(inputStreamBuilder: InputStreamBuilder): QuadTraversableView = throw new IllegalStateException("No specified RdfReader.")
  }

  def apply(file: File): RdfReader[RdfSource] = file.getName.replaceAll(".*\\.", "").toLowerCase match {
    case "nt" => Lang.NT
    case "nq" => Lang.NQ
    case "ttl" => Lang.TTL
    case "json" => Lang.JSONLD
    case "xml" => Lang.RDFXML
    case "trig" => Lang.TRIG
    case "trix" => Lang.TRIX
    case "tsv" => Tsv
    case x => throw new IllegalArgumentException(s"Unsupported RDF format for reading: $x")
  }

}
