package com.github.propi.rdfrules.data.formats

import com.github.propi.rdfrules.data.RdfSource.Tsv.ParsingMode
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.data.ops.PrefixesOps
import com.github.propi.rdfrules.utils.{BasicFunctions, InputStreamBuilder, OutputStreamBuilder}

import java.io.{BufferedInputStream, BufferedOutputStream, PrintWriter}
import scala.io.Source
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 14. 1. 2018.
  */
trait Tsv {

  private def stripResource(x: String) = x.trim.stripPrefix("<").stripSuffix(">").trim // replaceAll("[\\u0000-\\u0020]|[<>\"{}|^`\\\\]", "")

  private def stripMargins(x: String): String = x.substring(1, x.length - 1).trim

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

  private val prefixes = Array(
    Prefix.Full("rdfs", "http://www.w3.org/2000/01/rdf-schema#"),
    Prefix.Full("xsd", "http://www.w3.org/2001/XMLSchema#"),
    Prefix.Full("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#"),
    Prefix.Full("owl", "http://www.w3.org/2002/07/owl#")
  )

  private def stringToPrefixedUri(x: String): Option[TripleItem.Uri] = {
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

  private def stringToLongUri(x: String): Option[TripleItem.Uri] = {
    if (x.startsWith("<") && x.endsWith(">")) {
      Some(TripleItem.LongUri(stripMargins(x)))
    } else {
      None
    }
  }

  private def stringToNumber(x: String): Option[TripleItem] = {
    if (x.nonEmpty && (x.head.isDigit || x.head == '-')) {
      BasicFunctions.parseNumber(x).map {
        case Left(x) => TripleItem.Number(x)
        case Right(x) => TripleItem.Number(x)
      }
    } else {
      None
    }
  }

  private def stringToText(x: String): Option[TripleItem] = {
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

  class Formatter private[Tsv](parsingMode: ParsingMode) {
    val stringifyTripleItem: TripleItem => String = parsingMode match {
      case ParsingMode.Raw => {
        case TripleItem.LongUri(x) => x
        case x: TripleItem.PrefixedUri => stringifyTripleItem(x.toLongUri)
        case TripleItem.BlankNode(x) => stringifyTripleItem(TripleItem.LongUri(x))
        case TripleItem.Text(x) => x
        case x => x.toString
      }
      case ParsingMode.ParsedUris => {
        case TripleItem.LongUri(x) => s"<${stripResource(x)}>"
        case x: TripleItem.PrefixedUri => stringifyTripleItem(x.toLongUri)
        case TripleItem.BlankNode(x) => stringifyTripleItem(TripleItem.LongUri(x))
        case TripleItem.Text(x) => x
        case x => x.toString
      }
      case ParsingMode.ParsedLiterals => {
        case TripleItem.LongUri(x) => s"<${stripResource(x)}>"
        case x: TripleItem.PrefixedUri => stringifyTripleItem(x.toLongUri)
        case TripleItem.BlankNode(x) => stringifyTripleItem(TripleItem.LongUri(x))
        case x => x.toString
      }
    }
  }

  class Parser private[Tsv](parsingMode: ParsingMode) {
    val parseUri: String => TripleItem.Uri = parsingMode match {
      case ParsingMode.Raw => TripleItem.LongUri
      case _ => x => stringToLongUri(x).orElse(stringToPrefixedUri(x)).getOrElse(TripleItem.LongUri(x))
    }

    val parseLiteral: String => TripleItem = parsingMode match {
      case ParsingMode.Raw | ParsingMode.ParsedUris => parseUri
      case _ => x =>
        stringToLongUri(x)
          .orElse(stringToText(x))
          .orElse(stringToPrefixedUri(x))
          .orElse(stringToNumber(x))
          .getOrElse(TripleItem.LongUri(x))
    }

    def parseTriple(line: String): Option[Triple] = {
      val triple = line.trim.split("\t")
      if (triple.length == 3) {
        val trimmedSubject = triple(0).trim
        val trimmedPredicate = triple(1).trim
        val strippedSubject = parseUri(trimmedSubject)
        val strippedPredicate = parseUri(trimmedPredicate)
        val strippedObject = triple(2).trim.stripSuffix(".").trim
        Some(Triple(
          strippedSubject,
          strippedPredicate,
          parseLiteral(strippedObject)
        ))
      } else {
        None
      }
    }
  }

  def tripleParser(parsingMode: ParsingMode): Parser = new Parser(parsingMode)

  implicit def tsvReader(rdfSource: RdfSource.Tsv): RdfReader = (inputStreamBuilder: InputStreamBuilder) => (f: Quad => Unit) => {
    val is = new BufferedInputStream(inputStreamBuilder.build)
    val source = Source.fromInputStream(is, "UTF-8")
    try {
      val parser = tripleParser(rdfSource.parsingMode)
      for (triple <- source.getLines().flatMap(parser.parseTriple)) {
        f(triple.toQuad)
      }
    } finally {
      source.close()
      is.close()
    }
  }

  implicit def tsvWriter(rdfSource: RdfSource.Tsv): RdfWriter = (col: PrefixesOps[_], outputStreamBuilder: OutputStreamBuilder) => {
    val writer = new PrintWriter(new BufferedOutputStream(outputStreamBuilder.build))
    try {
      val formatter = new Formatter(rdfSource.parsingMode)
      for (Quad(triple, _) <- col.quads) {
        writer.println(s"${formatter.stringifyTripleItem(triple.subject)}\t${formatter.stringifyTripleItem(triple.predicate)}\t${formatter.stringifyTripleItem(triple.`object`)}")
      }
    } finally {
      writer.close()
    }
  }

}

object Tsv extends Tsv