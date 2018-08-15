package com.github.propi.rdfrules.data

import org.apache.jena.riot.Lang

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 27. 6. 2017.
  */
sealed trait RdfSource

object RdfSource {

  object Tsv extends RdfSource

  case class JenaLang(lang: Lang) extends RdfSource

  def apply(extension: String): RdfSource = extension.toLowerCase match {
    case "nt" => JenaLang(Lang.NT)
    case "nq" => JenaLang(Lang.NQ)
    case "ttl" => JenaLang(Lang.TTL)
    case "json" => JenaLang(Lang.JSONLD)
    case "xml" => JenaLang(Lang.RDFXML)
    case "trig" => JenaLang(Lang.TRIG)
    case "trix" => JenaLang(Lang.TRIX)
    case "tsv" => Tsv
    case x => throw new IllegalArgumentException(s"Unsupported RDF format for reading: $x")
  }

}