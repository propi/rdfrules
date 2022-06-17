package com.github.propi.rdfrules.data

import com.github.propi.rdfrules.utils.ForEach

import java.io.{File, FileInputStream, InputStream}
import org.apache.jena.graph
import org.apache.jena.riot.system.StreamRDF
import org.apache.jena.riot.{Lang, RDFDataMgr}
import org.apache.jena.sparql.core

/**
  * Created by Vaclav Zeman on 7. 10. 2017.
  */
sealed trait Prefix {
  def prefix: String

  def nameSpace: String

  override def hashCode(): Int = nameSpace.hashCode

  override def equals(obj: Any): Boolean = obj match {
    case x: Prefix => nameSpace.equals(x.nameSpace)
    case _ => false
  }
}

object Prefix {

  def apply(prefix: String, nameSpace: String): Full = Full(prefix, nameSpace)

  def apply(nameSpace: String): Namespace = Namespace(nameSpace)

  case class Full(prefix: String, nameSpace: String) extends Prefix

  case class Namespace(nameSpace: String) extends Prefix {
    def prefix: String = ""
  }

  def apply(buildInputStream: => InputStream): ForEach[Prefix] = (f: Prefix => Unit) => {
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

  def apply(file: File): ForEach[Prefix] = apply(new FileInputStream(file))

}
