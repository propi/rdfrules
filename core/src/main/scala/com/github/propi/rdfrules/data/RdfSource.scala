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

  /*implicit def rdfSourceToRdfReader[T <: RdfSource](rdfSource: T)(implicit rdfReader: RdfReader[T]): RdfReader[T] = rdfReader

  implicit def rdfSourceToRdfWriter[T <: RdfSource](rdfSource: T)(implicit rdfWriter: RdfWriter[T]): RdfWriter[T] = rdfWriter*/

  /*sealed trait RdfSourceToLang[T <: RdfSource] {
    def apply(): Lang

    def format(flat: Boolean = false, ascii: Boolean = false): RDFFormat
  }

  implicit val ntToLang: RdfSourceToLang[RdfSource.Nt.type] = new RdfSourceToLang[RdfSource.Nt.type] {
    def apply(): Lang = Lang.NT

    def format(flat: Boolean, ascii: Boolean): RDFFormat = if (ascii) RDFFormat.NTRIPLES_ASCII else RDFFormat.NTRIPLES_UTF8
  }

  implicit val ttloLang: RdfSourceToLang[RdfSource.Ttl.type] = new RdfSourceToLang[RdfSource.Ttl.type] {
    def apply(): Lang = Lang.TTL

    def format(flat: Boolean, ascii: Boolean): RDFFormat = if (flat) RDFFormat.TURTLE_FLAT else RDFFormat.TURTLE_BLOCKS
  }*/

}
