package com.github.propi.rdfrules.data.formats

import java.io.{BufferedInputStream, BufferedOutputStream, PrintWriter}

import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.data.ops.PrefixesOps
import com.github.propi.rdfrules.utils.{InputStreamBuilder, OutputStreamBuilder}

import scala.annotation.tailrec
import scala.io.Source
import scala.language.implicitConversions
import scala.util.Try

/**
  * Created by Vaclav Zeman on 14. 1. 2018.
  */
trait Tsv {

  self: JenaLang =>

  private def stripResource(x: String) = x.trim.stripPrefix("<").stripSuffix(">").replaceAll("[\\u0000-\\u0020]|[<>\"{}|^`\\\\]", "")

  @tailrec
  private def stringifyTripleItem(x: TripleItem): String = x match {
    case TripleItem.LongUri(x) => s"<${stripResource(x)}>"
    case x: TripleItem.PrefixedUri => stringifyTripleItem(x.toLongUri)
    case TripleItem.BlankNode(x) => stringifyTripleItem(TripleItem.LongUri(x))
    case x => x.toString
  }

  /*implicit def tsvReader(rdfSource: RdfSource.Tsv.type): RdfReader = (inputStreamBuilder: InputStreamBuilder) => new Traversable[Quad] {
    def foreach[U](f: Quad => U): Unit = {
      val is = new BufferedInputStream(inputStreamBuilder.build)
      val source = Source.fromInputStream(is, "UTF-8")
      try {
        val it = Iterator(
          "@base <http://tsv/> .",
          "@prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#> .",
          "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .",
          "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> .",
          "@prefix owl: <http://www.w3.org/2002/07/owl#> ."
        ).map(_ + "\n").flatMap(_.getBytes("UTF-8")) ++ source.getLines().map(_.trim.split("\t")).collect {
          case Array(s, p, o) =>
            val strippedObject = o.trim.replaceFirst("\\s*\\.$", "")
            Triple(
              TripleItem.Uri(stripResource(s)),
              TripleItem.Uri(stripResource(p)),
              if (strippedObject.headOption.contains('<') && strippedObject.lastOption.contains('>'))
                TripleItem.Uri(stripResource(strippedObject))
              else
                TripleItem.Text(strippedObject)
            )
        }.flatMap { triple =>
          val o = triple.`object` match {
            case TripleItem.Text(value) => value
            case x => x.toString
          }
          (triple.subject.toString + " " + triple.predicate.toString + " " + o + " .\n").getBytes("UTF-8")
        }
        jenaLangToRdfReader(Lang.TTL).fromInputStream(new InputStream {
          def read(): Int = if (it.hasNext) it.next().toInt else -1
        }).map { quad =>
          def shortenUri[T <: TripleItem](tripleItem: T): T = tripleItem match {
            case TripleItem.LongUri(x) => TripleItem.Uri(x.stripPrefix("http://tsv/")).asInstanceOf[T]
            case TripleItem.PrefixedUri(p, ln) if p.nameSpace == "http://tsv/" => TripleItem.Uri(ln).asInstanceOf[T]
            case x => x
          }

          quad.copy(triple = quad.triple.copy(
            subject = shortenUri(quad.triple.subject),
            predicate = shortenUri(quad.triple.predicate),
            `object` = shortenUri(quad.triple.`object`)
          ))
        }.foreach(f)
      } finally {
        source.close()
        is.close()
      }
    }
  }.view*/

  implicit def tsvReader(rdfSource: RdfSource.Tsv.type): RdfReader = (inputStreamBuilder: InputStreamBuilder) => new Traversable[Quad] {
    def foreach[U](f: Quad => U): Unit = {
      val is = new BufferedInputStream(inputStreamBuilder.build)
      val source = Source.fromInputStream(is, "UTF-8")
      try {
        val prefixes = Array(
          Prefix.Full("rdfs", "http://www.w3.org/2000/01/rdf-schema#"),
          Prefix.Full("xsd", "http://www.w3.org/2001/XMLSchema#"),
          Prefix.Full("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
          Prefix.Full("owl", "http://www.w3.org/2002/07/owl#")
        )
        val PrefixedMatcher = "(.+?):(.+)".r

        def stringToPrefixedUri(x: String): Option[TripleItem.Uri] = {
          x match {
            case PrefixedMatcher(prefix, localName) =>
              if (prefix == "_") {
                Some(TripleItem.BlankNode(localName))
              } else {
                prefixes.find(_.prefix == prefix).map(p => TripleItem.PrefixedUri(p, localName))
              }
            case _ => None
          }
        }

        def stringToUri(x: String): TripleItem.Uri = stringToPrefixedUri(x).getOrElse(TripleItem.LongUri(x))

        source.getLines().map(_.trim.split("\t")).collect {
          case Array(s, p, o) =>
            val strippedSubject = stringToUri(stripResource(s))
            val strippedPredicate = stringToUri(stripResource(p))
            val strippedObject = o.trim.replaceFirst("\\s*\\.\\s*$", "")
            Triple(
              strippedSubject,
              strippedPredicate,
              if (strippedObject.headOption.contains('<') && strippedObject.lastOption.contains('>')) {
                stringToUri(stripResource(strippedObject))
              } else {
                stringToPrefixedUri(strippedObject).getOrElse(
                  Try(TripleItem.Number(strippedObject.toInt))
                    .orElse(Try(TripleItem.Number(strippedObject.toDouble)))
                    .getOrElse(TripleItem.Text(strippedObject))
                )
              }
            ).toQuad
        }.foreach(f)
      } finally {
        source.close()
        is.close()
      }
    }
  }.view

  implicit def tsvWriter(rdfSource: RdfSource.Tsv.type): RdfWriter = (col: PrefixesOps[_], outputStreamBuilder: OutputStreamBuilder) => {
    val writer = new PrintWriter(new BufferedOutputStream(outputStreamBuilder.build))
    try {
      for (Quad(triple, _) <- col.quads) {
        writer.println(s"${stringifyTripleItem(triple.subject)}\t${stringifyTripleItem(triple.predicate)}\t${stringifyTripleItem(triple.`object`)}")
      }
    } finally {
      writer.close()
    }
  }

}