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

  def apply(buildInputStream: => InputStream): Traversable[Prefix] = new Traversable[Prefix] {
    def foreach[U](f: Prefix => U): Unit = {
      val is = buildInputStream
      try {
        RDFDataMgr.parse(
          new StreamRDF {
            def prefix(prefix: String, iri: String): Unit = f(Prefix(prefix, iri))

            def start(): Unit = {}

            def quad(quad: core.Quad): Unit = {}

            def triple(triple: graph.Triple): Unit = {}

            def finish(): Unit = {}

            def base(base: String): Unit = {}
          },
          is,
          Lang.TTL
        )
      } finally {
        is.close()
      }
    }
  }

  def apply(file: File): Traversable[Prefix] = apply(new FileInputStream(file))

}
