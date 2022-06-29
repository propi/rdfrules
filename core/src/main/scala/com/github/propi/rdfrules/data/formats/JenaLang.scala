package com.github.propi.rdfrules.data.formats

import com.github.propi.rdfrules
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.data.ops.PrefixesOps
import com.github.propi.rdfrules.utils.{InputStreamBuilder, OutputStreamBuilder}
import org.apache.jena.graph
import org.apache.jena.graph.{Node_Blank, Node_Literal, Node_URI}
import org.apache.jena.rdf.model.impl.Util
import org.apache.jena.riot.system.{StreamRDF, StreamRDFWriter}
import org.apache.jena.riot.{Lang, RDFFormat, RDFLanguages, RDFParser}
import org.apache.jena.sparql.core.Quad

import java.io.{BufferedInputStream, BufferedOutputStream}
import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 14. 1. 2018.
  */
trait JenaLang {

  private class StreamRdfImpl[U](f: rdfrules.data.Quad => U) extends StreamRDF {
    private val prefixes = collection.mutable.Map.empty[String, Prefix]

    private def uriToTripleItem(x: Node_URI): TripleItem.Uri = {
      if (prefixes.isEmpty) {
        TripleItem.LongUri(x.getURI)
      } else {
        val i = Util.splitNamespaceXML(x.getURI)
        val nameSpace = x.getURI.substring(0, i)
        prefixes.get(nameSpace).map(p => TripleItem.PrefixedUri(p, x.getURI.substring(i))).getOrElse(TripleItem.LongUri(x.getURI))
      }
    } //Try(prefixes.iterator.filter(p => x.getURI.startsWith(p.nameSpace)).maxBy(_.nameSpace.length)).map(p => TripleItem.PrefixedUri(p, x.getURI.substring(p.nameSpace.length))).getOrElse(TripleItem.LongUri(x.getURI))

    def prefix(prefix: String, iri: String): Unit = prefixes += (iri -> Prefix(prefix, iri))

    def start(): Unit = {}

    def quad(quad: Quad): Unit = {
      val triple = rdfrules.data.Triple(
        quad.getSubject match {
          case x: Node_URI => uriToTripleItem(x)
          case x: Node_Blank => TripleItem.BlankNode(x.getBlankNodeId.getLabelString)
          case _ => throw new IllegalArgumentException
        },
        quad.getPredicate match {
          case x: Node_URI => uriToTripleItem(x)
          case _ => throw new IllegalArgumentException
        },
        quad.getObject match {
          case x: Node_Literal =>
            val literal = x.getLiteral
            if (literal.isWellFormed) {
              literal.getValue match {
                case x: java.lang.Integer => TripleItem.Number(x.intValue())
                case x: java.lang.Double => TripleItem.Number(x.doubleValue())
                case x: java.lang.Short => TripleItem.Number(x.shortValue())
                case x: java.lang.Float => TripleItem.Number(x.floatValue())
                case x: java.lang.Long => TripleItem.Number(x.longValue())
                case x: java.lang.Byte => TripleItem.Number(x.byteValue())
                case x: java.lang.Boolean => TripleItem.BooleanValue(x.booleanValue())
                case x: java.math.BigInteger => TripleItem.Number(x.longValueExact())
                case x: java.math.BigDecimal => TripleItem.Number(x.doubleValue())
                case _ =>
                  val text = literal.getLexicalForm
                  TripleItem.Interval(text).getOrElse(TripleItem.Text(text))
              }
            } else {
              val text = literal.getLexicalForm
              TripleItem.Interval(text).getOrElse(TripleItem.Text(text))
            }
          case x: Node_URI => uriToTripleItem(x)
          case x: Node_Blank => TripleItem.BlankNode(x.getBlankNodeId.getLabelString)
          case _ => throw new IllegalArgumentException
        }
      )
      f(quad.getGraph match {
        case x: Node_URI => triple.toQuad(uriToTripleItem(x))
        case _ => triple.toQuad
      })
    }

    def triple(triple: graph.Triple): Unit = quad(new Quad(null, triple))

    def finish(): Unit = {}

    def base(base: String): Unit = prefixes += (base -> Prefix(base))
  }

  implicit def jenaLangToRdfReader(jenaLang: Lang): RdfReader = (inputStreamBuilder: InputStreamBuilder) => (f: rdfrules.data.Quad => Unit) => {
    val is = new BufferedInputStream(inputStreamBuilder.build)
    try {
      RDFParser.create()
        .source(is)
        .base(null)
        .lang(jenaLang)
        .context(null)
        .checking(false)
        .parse(new StreamRdfImpl(f))
    } finally {
      is.close()
    }
  }

  implicit def jenaFormatToRdfWriter(rdfFormat: RDFFormat): RdfWriter = (col: PrefixesOps[_], outputStreamBuilder: OutputStreamBuilder) => {
    val os = new BufferedOutputStream(outputStreamBuilder.build)
    val stream = StreamRDFWriter.getWriterStream(os, rdfFormat)
    try {
      stream.start()
      for (prefix <- col.userDefinedPrefixes) {
        stream.prefix(prefix.prefix, prefix.nameSpace)
      }
      rdfFormat.getLang match {
        case RDFLanguages.N3 | RDFLanguages.NT | RDFLanguages.NTRIPLES | RDFLanguages.TTL | RDFLanguages.TURTLE =>
          col.quads.foreach(quad => stream.triple(quad.triple))
        case _ =>
          col.quads.foreach(quad => stream.quad(quad))
      }
    } finally {
      stream.finish()
      os.close()
    }
  }

}