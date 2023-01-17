package com.github.propi.rdfrules.data

import org.apache.jena.riot.{Lang, RDFFormat}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 27. 6. 2017.
  */
sealed trait RdfSource

object RdfSource {

  case class Tsv(parsingMode: Tsv.ParsingMode) extends RdfSource

  object Tsv {
    sealed trait ParsingMode

    object ParsingMode {
      case object Raw extends ParsingMode

      case object ParsedUris extends ParsingMode

      case object ParsedLiterals extends ParsingMode
    }
  }

  object Sql extends RdfSource

  object Cache extends RdfSource

  case class JenaLang(lang: Lang) extends RdfSource {
    def toRDFFormat: RDFFormat = if (lang == Lang.NT) {
      RDFFormat.NTRIPLES_UTF8
    } else if (lang == Lang.NQ) {
      RDFFormat.NQUADS_UTF8
    } else if (lang == Lang.TTL) {
      RDFFormat.TURTLE_FLAT
    } else if (lang == Lang.TRIG) {
      RDFFormat.TRIG_FLAT
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
    case "json" | "jsonld" => JenaLang(Lang.JSONLD)
    case "xml" | "rdf" | "owl" => JenaLang(Lang.RDFXML)
    case "trig" => JenaLang(Lang.TRIG)
    case "trix" => JenaLang(Lang.TRIX)
    case "tsv" => Tsv(Tsv.ParsingMode.Raw)
    case "sql" => Sql
    case "cache" => Cache
    case x => throw new IllegalArgumentException(s"Unsupported RDF format for reading: $x")
  }

  implicit def rdfSourceToRdfReader(rdfSource: RdfSource): RdfReader = rdfSource match {
    case JenaLang(lang) => lang
    case tsv: Tsv => tsv
    case Sql => Sql
    case Cache => Cache
  }

  implicit def rdfSourceToRdfWriter(rdfSource: RdfSource): RdfWriter = rdfSource match {
    case x: JenaLang => x.toRDFFormat
    case tsv: Tsv => tsv
    case Sql => Sql
    case Cache => Cache
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