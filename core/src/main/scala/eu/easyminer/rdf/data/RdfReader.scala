package eu.easyminer.rdf.data

import java.io.{File, FileInputStream}

import eu.easyminer.rdf.data.Quad.QuadTraversableView
import eu.easyminer.rdf.data.RdfSource.{JenaLang, Tsv}
import eu.easyminer.rdf.data.formats.JenaLang._
import eu.easyminer.rdf.data.formats.Tsv._
import eu.easyminer.rdf.utils.InputStreamBuilder
import org.apache.jena.riot.Lang

/**
  * Created by Vaclav Zeman on 27. 6. 2017.
  */
trait RdfReader[+T <: RdfSource] {
  def fromInputStream(inputStreamBuilder: InputStreamBuilder): QuadTraversableView

  def fromFile(file: File): QuadTraversableView = fromInputStream(new FileInputStream(file))
}

object RdfReader {

  def apply(file: File): RdfReader[RdfSource] = file.getName.replaceAll(".*\\.", "").toLowerCase match {
    case "nt" => JenaLang(Lang.NT)
    case "nq" => JenaLang(Lang.NQ)
    case "ttl" => JenaLang(Lang.TTL)
    case "json" => JenaLang(Lang.JSONLD)
    case "xml" => JenaLang(Lang.RDFXML)
    case "trig" => JenaLang(Lang.TRIG)
    case "trix" => JenaLang(Lang.TRIX)
    case "tsv" => Tsv
    case _ => throw new IllegalArgumentException
  }

}
