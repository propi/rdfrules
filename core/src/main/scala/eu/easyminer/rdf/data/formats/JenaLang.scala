package eu.easyminer.rdf.data.formats

import eu.easyminer.rdf.data
import eu.easyminer.rdf.data.Triple.TripleTraversableView
import eu.easyminer.rdf.data._
import eu.easyminer.rdf.utils.{InputStreamBuilder, OutputStreamBuilder}
import org.apache.jena.graph
import org.apache.jena.graph.{NodeFactory, Node_Blank, Node_Literal, Node_URI}
import org.apache.jena.riot.{RDFDataMgr, RDFFormat}
import org.apache.jena.riot.system.{StreamRDF, StreamRDFWriter}
import org.apache.jena.sparql.core.Quad

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 14. 1. 2018.
  */
object JenaLang {

  private class StreamRdfImpl[U](f: data.Quad => U) extends StreamRDF {
    private val prefixes = collection.mutable.Map.empty[String, String]

    private def uriToTripleItem(x: Node_URI): TripleItem.Uri = prefixes.get(x.getNameSpace).map(TripleItem.PrefixedUri(_, x.getNameSpace, x.getLocalName)).getOrElse(TripleItem.LongUri(x.getURI))

    def prefix(prefix: String, iri: String): Unit = prefixes += (iri -> prefix)

    def start(): Unit = {}

    def quad(quad: Quad): Unit = f(
      data.Quad(
        Triple(
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
            case x: Node_Literal => x.getLiteralValue match {
              case x: java.lang.Integer => TripleItem.Number(x.intValue())
              case x: java.lang.Double => TripleItem.Number(x.doubleValue())
              case x: java.lang.Short => TripleItem.Number(x.shortValue())
              case x: java.lang.Float => TripleItem.Number(x.floatValue())
              case x: java.lang.Long => TripleItem.Number(x.longValue())
              case x: java.lang.Byte => TripleItem.Number(x.byteValue())
              case x: java.lang.Boolean => TripleItem.BooleanValue(x.booleanValue())
              case x: java.math.BigInteger => TripleItem.Number(x.longValueExact())
              case x: java.math.BigDecimal => TripleItem.Number(x.doubleValue())
              case _ => TripleItem.Text(x.getLiteralLexicalForm)
            }
            case x: Node_URI => uriToTripleItem(x)
            case x: Node_Blank => TripleItem.BlankNode(x.getBlankNodeId.getLabelString)
            case _ => throw new IllegalArgumentException
          }
        ),
        Some(quad.getGraph).collect {
          case x: Node_URI => uriToTripleItem(x)
        }.getOrElse(TripleItem.LongUri(""))
      )
    )

    def triple(triple: graph.Triple): Unit = quad(new Quad(NodeFactory.createURI(""), triple))

    def finish(): Unit = {}

    def base(base: String): Unit = {}
  }

  implicit def jenaLangToRdfReader(jenaLang: RdfSource.JenaLang): RdfReader[RdfSource.JenaLang] = (inputStreamBuilder: InputStreamBuilder) => new Traversable[data.Quad] {
    def foreach[U](f: (data.Quad) => U): Unit = {
      val is = inputStreamBuilder.build
      try {
        RDFDataMgr.parse(new StreamRdfImpl(f), is, jenaLang.lang)
      } finally {
        is.close()
      }
    }
  }.view

  implicit def jenaFormatToRdfWriter(rdfFormat: RDFFormat): RdfWriter[RdfSource.JenaLang] = (triples: TripleTraversableView, outputStreamBuilder: OutputStreamBuilder) => {
    val os = outputStreamBuilder.build
    val stream = StreamRDFWriter.getWriterStream(os, rdfFormat)
    try {
      stream.start()
      for (prefix <- triples.toPrefixes) {
        stream.prefix(prefix.prefix, prefix.nameSpace)
      }
      triples.foreach(triple => stream.triple(triple))
    } finally {
      stream.finish()
      os.close()
    }
  }

}