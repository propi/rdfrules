package com.github.propi.rdfrules.data

import org.apache.jena.riot.{Lang, RDFFormat}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 27. 6. 2017.
  */
sealed trait RdfSource

object RdfSource {

  object Tsv extends RdfSource

  object Sql extends RdfSource

  case class JenaLang(lang: Lang) extends RdfSource {
    def toRDFFormat: RDFFormat = if (lang == Lang.NT) {
      RDFFormat.NTRIPLES_UTF8
    } else if (lang == Lang.NQ) {
      RDFFormat.NQUADS_UTF8
    } else if (lang == Lang.TTL) {
      RDFFormat.TURTLE_FLAT
    } else if (lang == Lang.TRIG) {
      RDFFormat.TRIG_BLOCKS
    } else if (lang == Lang.TRIX) {
      RDFFormat.TRIX
    } else {
      new RDFFormat(lang)
    }
  }

  def apply(extension: String): RdfSource = extension.toLowerCase match {
    case "nt" => JenaLang(Lang.NT)
    case "nq" => JenaLang(Lang.NQ)
    case "ttl" => JenaLang(Lang.TTL)
    case "json" => JenaLang(Lang.JSONLD)
    case "xml" => JenaLang(Lang.RDFXML)
    case "trig" => JenaLang(Lang.TRIG)
    case "trix" => JenaLang(Lang.TRIX)
    case "tsv" => Tsv
    case "sql" => Sql
    case x => throw new IllegalArgumentException(s"Unsupported RDF format for reading: $x")
  }

  implicit def rdfSourceToRdfReader(rdfSource: RdfSource): RdfReader = rdfSource match {
    case JenaLang(lang) => lang
    case Tsv => Tsv
    case Sql => Sql
  }

  implicit def rdfSourceToRdfWriter(rdfSource: RdfSource): RdfWriter = rdfSource match {
    case x: JenaLang => x.toRDFFormat
    case Tsv => Tsv
    case Sql => Sql
  }

  sealed trait CompressedRdfSource {
    val compression: Compression
  }

  object CompressedRdfSource {

    case class Basic(rdfSource: RdfSource, compression: Compression) extends CompressedRdfSource

    case class RdfFormat(format: RDFFormat, compression: Compression) extends CompressedRdfSource

  }

  implicit class PimpedRdfSource(val rdfSource: RdfSource) extends AnyVal {
    def compressedBy(compression: Compression): CompressedRdfSource = CompressedRdfSource.Basic(rdfSource, compression)
  }

  implicit class PimpedRdfFormat(val format: RDFFormat) extends AnyVal {
    def compressedBy(compression: Compression): CompressedRdfSource = CompressedRdfSource.RdfFormat(format, compression)
  }

}