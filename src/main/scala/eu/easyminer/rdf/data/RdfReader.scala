package eu.easyminer.rdf.data

import java.io.{File, FileInputStream, InputStream}

import eu.easyminer.rdf.data.RdfReader.TripleTraversableView
import eu.easyminer.rdf.data.RdfSource.{Nt, RdfSourceToLang, Tsv, Ttl}
import org.apache.jena.graph
import org.apache.jena.graph.{Node_Blank, Node_Literal, Node_URI}
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.riot.system.StreamRDF
import org.apache.jena.sparql.core.Quad

import scala.collection.TraversableView
import scala.io.Source

/**
  * Created by Vaclav Zeman on 27. 6. 2017.
  */
trait RdfReader[T <: RdfSource] {
  def fromInputStream(buildInputStream: => InputStream): TripleTraversableView

  def fromFile(file: File): TripleTraversableView = fromInputStream(new FileInputStream(file))
}

object RdfReader {

  type TripleTraversableView = TraversableView[Triple, Traversable[_]]

  private abstract class TripleTraversableViewImpl extends TripleTraversableView {
    protected def underlying: Traversable[_] = throw new UnsupportedOperationException("No collection for rdf traversable view")
  }

  implicit val Tsv2Triples: RdfReader[RdfSource.Tsv.type] = new RdfReader[RdfSource.Tsv.type] {
    def fromInputStream(buildInputStream: => InputStream): TripleTraversableView = new TripleTraversableViewImpl {
      def foreach[U](f: (Triple) => U): Unit = {
        val is = buildInputStream
        val source = Source.fromInputStream(is)
        try {
          val it = source.getLines().map(_.split("\t")).collect {
            case Array(s, p, o) => Triple(TripleItem.LongUri(s), TripleItem.LongUri(p), TripleItem.Text(o.stripSuffix(".")))
          }
          it.foreach(f)
        } finally {
          source.close()
          is.close()
        }
      }
    }
  }

  private class StreamRdfImpl[U](f: Triple => U) extends StreamRDF {
    private val prefixes = collection.mutable.Map.empty[String, String]

    private def uriToTripleItem(x: Node_URI): TripleItem.Uri = prefixes.get(x.getNameSpace).map(TripleItem.PrefixedUri(_, x.getNameSpace, x.getLocalName)).getOrElse(TripleItem.LongUri(x.getURI))

    def prefix(prefix: String, iri: String): Unit = prefixes += (iri -> prefix)

    def start(): Unit = {}

    def quad(quad: Quad): Unit = triple(quad.asTriple())

    def triple(triple: graph.Triple): Unit = f(
      Triple(
        triple.getSubject match {
          case x: Node_URI => uriToTripleItem(x)
          case x: Node_Blank => TripleItem.BlankNode(x.getBlankNodeId.getLabelString)
          case _ => throw new IllegalArgumentException
        },
        triple.getPredicate match {
          case x: Node_URI => uriToTripleItem(x)
          case _ => throw new IllegalArgumentException
        },
        triple.getObject match {
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
      )
    )

    def finish(): Unit = {}

    def base(base: String): Unit = {}
  }

  implicit def anyForLang2Triples[T <: RdfSource](implicit lang: RdfSourceToLang[T]): RdfReader[T] = new RdfReader[T] {
    def fromInputStream(buildInputStream: => InputStream): TripleTraversableView = new TripleTraversableViewImpl {
      def foreach[U](f: (Triple) => U): Unit = {
        val is = buildInputStream
        try {
          RDFDataMgr.parse(new StreamRdfImpl(f), is, lang())
        } finally {
          is.close()
        }
      }
    }
  }

  def apply(file: File): TripleTraversableView = {
    file.getName.replaceAll(".*\\.", "").toLowerCase match {
      case "nt" => Nt.fromFile(file)
      case "ttl" => Ttl.fromFile(file)
      case "tsv" => Tsv.fromFile(file)
      case _ => throw new IllegalArgumentException
    }
  }

}
