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

  private def stripResource(x: String) = x.trim.stripPrefix("<").stripSuffix(">").trim // replaceAll("[\\u0000-\\u0020]|[<>\"{}|^`\\\\]", "")

  private def stripMargins(x: String): String = x.substring(1, x.length - 1).trim

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

        def stringToPrefixedUri(x: String): Option[TripleItem.Uri] = {
          val sepIndex = x.indexOf(':')
          if (sepIndex >= 0) {
            val prefix = x.substring(0, sepIndex)
            val localName = if (x.length > (sepIndex + 1)) x.substring(sepIndex + 1) else ""
            if (prefix == "_") {
              Some(TripleItem.BlankNode(localName))
            } else {
              prefixes.find(_.prefix == prefix).map(p => TripleItem.PrefixedUri(p, localName)).orElse(Some(TripleItem.LongUri(x)))
            }
          } else {
            None
          }
        }

        def stringToLongUri(x: String): Option[TripleItem.Uri] = {
          if (x.startsWith("<") && x.endsWith(">")) {
            Some(TripleItem.LongUri(stripMargins(x)))
          } else {
            None
          }
        }

        def stringToNumber(x: String): Try[TripleItem] = {
          Try(TripleItem.Number(x.toInt)).orElse(Try(TripleItem.Number(x.toDouble)))
        }

        def stringToText(x: String): Option[TripleItem] = {
          if (x.startsWith("\"")) {
            if (x.endsWith("\"")) {
              Some(TripleItem.Text(stripMargins(x)))
            } else {
              val sepIndex = x.lastIndexOf("\"^^")
              if (sepIndex >= 0) {
                val text = x.substring(1, sepIndex)
                Some(stringToNumber(text).getOrElse(TripleItem.Text(text)))
              } else {
                None
              }
            }
          } else {
            None
          }
        }

        for (triple <- source.getLines().map(_.trim.split("\t"))) {
          if (triple.length == 3) {
            val trimmedSubject = triple(0).trim
            val trimmedPredicate = triple(1).trim
            val strippedSubject = stringToLongUri(trimmedSubject).orElse(stringToPrefixedUri(trimmedSubject)).getOrElse(TripleItem.LongUri(trimmedSubject))
            val strippedPredicate = stringToLongUri(trimmedPredicate).orElse(stringToPrefixedUri(trimmedPredicate)).getOrElse(TripleItem.LongUri(trimmedPredicate))
            val strippedObject = triple(2).trim.stripSuffix(".").trim
            val quad = Triple(
              strippedSubject,
              strippedPredicate,
              stringToLongUri(strippedObject)
                .orElse(stringToText(strippedObject))
                .orElse(stringToPrefixedUri(strippedObject))
                .getOrElse(stringToNumber(strippedObject).getOrElse(TripleItem.Text(strippedObject)))
            ).toQuad
            f(quad)
          }
        }
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