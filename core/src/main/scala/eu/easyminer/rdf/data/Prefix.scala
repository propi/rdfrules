package eu.easyminer.rdf.data

import java.io.{File, FileInputStream, InputStream}

import org.apache.jena.graph
import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.apache.jena.riot.system.StreamRDF
import org.apache.jena.sparql.core

/**
  * Created by Vaclav Zeman on 7. 10. 2017.
  */
case class Prefix(prefix: String, nameSpace: String)

object Prefix {

  def apply(buildInputStream: => InputStream): List[Prefix] = {
    val is = buildInputStream
    try {
      val prefixes = collection.mutable.Set.empty[Prefix]
      RDFDataMgr.parse(
        new StreamRDF {
          def prefix(prefix: String, iri: String): Unit = prefixes += Prefix(prefix, iri)

          def start(): Unit = {}

          def quad(quad: core.Quad): Unit = {}

          def triple(triple: graph.Triple): Unit = {}

          def finish(): Unit = {}

          def base(base: String): Unit = {}
        },
        is,
        Lang.TTL
      )
      prefixes.toList
    } finally {
      is.close()
    }
  }

  def apply(file: File): List[Prefix] = apply(new FileInputStream(file))

}
