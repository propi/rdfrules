package eu.easyminer.rdf.data

import org.apache.jena.graph

import scala.collection.TraversableView
import scala.language.implicitConversions

/**
  * Created by propan on 16. 4. 2017.
  */
case class Triple(subject: TripleItem.Uri, predicate: TripleItem.Uri, `object`: TripleItem)

object Triple {

  type TripleTraversableView = TraversableView[Triple, Traversable[_]]

  implicit def tripleToJenaTriple(triple: Triple): graph.Triple = new graph.Triple(triple.subject, triple.predicate, triple.`object`)

  implicit class PimpedTraversableTriple(triples: TripleTraversableView) {
    def toPrefixes: List[Prefix] = {
      val map = collection.mutable.HashMap.empty[String, String]
      for {
        triple <- triples
        TripleItem.PrefixedUri(prefix, nameSpace, _) <- List(triple.subject, triple.predicate, triple.`object`)
      } {
        map += (prefix -> nameSpace)
      }
      map.iterator.map(x => Prefix(x._1, x._2)).toList
    }

    def zipWithIndex: TraversableView[(Triple, Int), Traversable[_]] = new triples.Transformed[(Triple, Int)] {
      def foreach[U](f: ((Triple, Int)) => U): Unit = {
        var i = 0
        triples.foreach { triple =>
          f(triple -> i)
          i = i + 1
        }
      }
    }
  }

}