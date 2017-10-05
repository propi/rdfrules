package eu.easyminer.rdf.data

import org.apache.jena.graph

import scala.language.implicitConversions

/**
  * Created by propan on 16. 4. 2017.
  */
case class Triple(subject: TripleItem.Uri, predicate: TripleItem.Uri, `object`: TripleItem)

object Triple {

  implicit def tripleToJenaTriple(triple: Triple): graph.Triple = new graph.Triple(triple.subject, triple.predicate, triple.`object`)

  implicit class PimpedTraversableTriple(triples: Traversable[Triple]) {
    def toPrefixes: List[(String, String)] = {
      val map = collection.mutable.HashMap.empty[String, String]
      for {
        triple <- triples
        TripleItem.PrefixedUri(prefix, nameSpace, _) <- List(triple.subject, triple.predicate, triple.`object`)
      } {
        map += (prefix -> nameSpace)
      }
      map.toList
    }
  }

}