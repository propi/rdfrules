package eu.easyminer.rdf.data

import eu.easyminer.rdf.utils.{Printer, Stringifier}
import org.apache.jena.graph

import scala.language.implicitConversions

/**
  * Created by propan on 16. 4. 2017.
  */
case class Triple(subject: TripleItem.Uri, predicate: TripleItem.Uri, `object`: TripleItem)

object Triple {

  implicit def tripleToJenaTriple(triple: Triple): graph.Triple = new graph.Triple(triple.subject, triple.predicate, triple.`object`)

  implicit class PimpedTraversableTriple(triples: Traversable[Triple]) {
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
  }

  implicit val tripleStringifier: Stringifier[Triple] = (v: Triple) => v.subject + "  " + v.predicate + "  " + v.`object`

  implicit def tripleDataPrinter(implicit str: Stringifier[Triple]): Printer[Triple] = (v: Triple) => println(str.toStringValue(v))

}