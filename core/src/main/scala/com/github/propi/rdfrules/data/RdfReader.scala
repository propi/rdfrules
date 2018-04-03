package com.github.propi.rdfrules.data

import java.io.{File, FileInputStream}

import com.github.propi.rdfrules.data.Quad.QuadTraversableView
import com.github.propi.rdfrules.data.RdfSource.{JenaLang, Tsv}
import com.github.propi.rdfrules.utils.InputStreamBuilder
import org.apache.jena.riot.Lang
import com.github.propi.rdfrules.data.formats.JenaLang._
import com.github.propi.rdfrules.data.formats.Tsv._

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
