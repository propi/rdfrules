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

  implicit val Tsv2Triples: RdfReader[RdfSource.Tsv.type] = new RdfReader[RdfSource.Tsv.type] {
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

  sealed trait RdfSourceToLang[T <: RdfSource] {
    def apply(): Lang
  }

  implicit val ntToLang: RdfSourceToLang[RdfSource.Nt.type] = new RdfSourceToLang[RdfSource.Nt.type] {
    def apply(): Lang = Lang.NT
  }

  implicit def anyForLang2Triples[T <: RdfSource](implicit lang: RdfSourceToLang[T]): RdfReader[T] = new RdfReader[T] {
    def fromInputStream[A](is: InputStream)(f: (Iterator[Triple]) => A): A = try {
      val it = RDFDataMgr.createIteratorTriples(is, lang(), null).asScala.map { triple =>
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
