package eu.easyminer.rdf.data

import java.io.{File, FileInputStream, InputStream}

import org.apache.jena.graph.{Node_Literal, Node_URI}
import org.apache.jena.riot.{Lang, RDFDataMgr}

import scala.collection.JavaConverters._
import scala.io.Source

/**
  * Created by Vaclav Zeman on 27. 6. 2017.
  */
trait RdfReader[T <: RdfSource] {
  def fromInputStream[A](is: InputStream)(f: Iterator[Triple] => A): A

  def fromFile[A](file: File)(f: Iterator[Triple] => A): A = fromInputStream(new FileInputStream(file))(f)
}

object RdfReader {

  implicit val Tsv2Triples = new RdfReader[RdfSource.Tsv.type] {
    def fromInputStream[A](is: InputStream)(f: Iterator[Triple] => A): A = {
      val source = Source.fromInputStream(is)
      try {
        val it = source.getLines().map(_.split("\t")).collect {
          case Array(s, p, o) => Triple(s, p, TripleObject.NominalLiteral(o.stripSuffix(".")))
        }
        f(it)
      } finally {
        source.close()
        is.close()
      }
    }
  }

  implicit val Nt2Triples = new RdfReader[RdfSource.Nt.type] {
    def fromInputStream[A](is: InputStream)(f: (Iterator[Triple]) => A): A = try {
      val it = RDFDataMgr.createIteratorTriples(is, Lang.NT, null).asScala.map { triple =>
        val `object` = triple.getObject match {
          case x: Node_Literal => TripleObject.NominalLiteral(x.getLiteral.getLexicalForm)
          case x: Node_URI => TripleObject.Uri(x.getURI)
          case _ => throw new IllegalArgumentException
        }
        Triple(triple.getSubject.getURI, triple.getPredicate.getURI, `object`)
      }
      f(it)
    } finally {
      is.close()
    }
  }

}
