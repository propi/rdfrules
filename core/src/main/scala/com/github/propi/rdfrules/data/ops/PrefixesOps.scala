package com.github.propi.rdfrules.data.ops

import java.io.{File, InputStream}

import com.github.propi.rdfrules.data.{Prefix, Quad, Triple, TripleItem}
import com.github.propi.rdfrules.utils.PreservedTraversable
import com.github.propi.rdfrules.utils.extensions.TraversableOnceExtension._

/**
  * Created by Vaclav Zeman on 14. 1. 2020.
  */
trait PrefixesOps[Coll] extends QuadsOps[Coll] {

  protected def transformPrefixesAndColl(prefixes: Traversable[Prefix], col: Traversable[Quad]): Coll

  def userDefinedPrefixes: Traversable[Prefix]

  def setPrefixes(prefixes: Traversable[Prefix]): Coll = transformPrefixesAndColl(prefixes, new Traversable[Quad] {
    def foreach[U](f: Quad => U): Unit = {
      val map = prefixes.view.map(x => x.nameSpace -> x).toMap

      def tryToPrefix(uri: TripleItem.Uri) = uri match {
        case x: TripleItem.LongUri =>
          val (nameSpace, localName) = x.explode
          map.get(nameSpace).map(TripleItem.PrefixedUri(_, localName)).getOrElse(x)
        case x: TripleItem.PrefixedUri =>
          map.get(x.prefix.nameSpace).map(prefix => x.copy(prefix = prefix)).getOrElse(x)
        case x => x
      }

      for (quad <- quads) {
        val updatedTriple = Triple(
          tryToPrefix(quad.triple.subject),
          tryToPrefix(quad.triple.predicate),
          quad.triple.`object` match {
            case x: TripleItem.Uri => tryToPrefix(x)
            case x => x
          }
        )
        f(Quad(updatedTriple, tryToPrefix(quad.graph)))
      }
    }
  })

  /**
    * Append user defined prefixes and transform all triples by these prefixes.
    * Streaming transformation.
    *
    * @param addingPrefixes prefixes
    * @return
    */
  def addPrefixes(addingPrefixes: Traversable[Prefix]): Coll = {
    setPrefixes((userDefinedPrefixes.view ++ addingPrefixes.view).distinct)
  }

  def addPrefixes(buildInputStream: => InputStream): Coll = addPrefixes(Prefix(buildInputStream))

  def addPrefixes(file: File): Coll = addPrefixes(Prefix(file))

  def addPrefixes(file: String): Coll = addPrefixes(new File(file))

  /**
    * Remove defined prefixes and transform all triples to remove these prefixes.
    * Streaming transformation.
    *
    * @param removingPrefixes prefixes to remove
    * @return
    */
  def removePrefixes(removingPrefixes: Set[Prefix]): Coll = {
    def tryToRemovePrefix(uri: TripleItem.Uri) = uri match {
      case x: TripleItem.PrefixedUri if removingPrefixes(x.prefix) => x.toLongUri
      case x => x
    }

    transformPrefixesAndColl(userDefinedPrefixes.view.filter(!removingPrefixes(_)), quads.map { quad =>
      val updatedTriple = Triple(
        tryToRemovePrefix(quad.triple.subject),
        tryToRemovePrefix(quad.triple.predicate),
        quad.triple.`object` match {
          case x: TripleItem.Uri => tryToRemovePrefix(x)
          case x => x
        }
      )
      Quad(updatedTriple, tryToRemovePrefix(quad.graph))
    })
  }

  /**
    * Transform all URIs to PrefixedUris with distinct prefixes
    * All prefixes with short name are used.
    * Otherwise nameSpace without short name is transformed only to nameSpace prefix with empty short string
    *
    * @return Coll
    */
  def withPrefixedUris: Coll = {
    transformQuads(new Traversable[Quad] {
      def foreach[U](f: Quad => U): Unit = {
        val prefixes = collection.mutable.Map.empty[String, Prefix]

        def uriToPrefixedUri(uri: TripleItem.Uri) = uri match {
          case uri: TripleItem.LongUri =>
            val (nameSpace, localName) = uri.explode
            if (nameSpace.isEmpty) {
              uri
            } else {
              val prefix = prefixes.getOrElseUpdate(nameSpace, Prefix(nameSpace))
              TripleItem.PrefixedUri(prefix, localName)
            }
          case x => x
        }

        def tripleItemToPrefixedUri(tripleItem: TripleItem) = tripleItem match {
          case uri: TripleItem.Uri => uriToPrefixedUri(uri)
          case x => x
        }

        for (quad <- quads) {
          f(Quad(
            Triple(
              uriToPrefixedUri(quad.triple.subject),
              uriToPrefixedUri(quad.triple.predicate),
              tripleItemToPrefixedUri(quad.triple.`object`)
            ),
            uriToPrefixedUri(quad.graph)
          ))
        }
      }
    })
  }

  /**
    * Return all user defined prefixes with all prefixes extracted from triples (e.g. from ttl).
    * Streaming transformation.
    *
    * @return prefixes
    */
  def resolvedPrefixes: Traversable[Prefix] = {
    PreservedTraversable((userDefinedPrefixes.view ++ quads.prefixes.view).distinct)
  }

  /**
    * Extract all prefixes form triples and append them into the user defined prefixes.
    * This is the streaming transformation operation but during some action the triple collection must be browsed twice!!!
    *  - first for the prefix resolving and second for the collection browsing
    *
    * @return
    */
  def resolvePrefixes: Coll = {
    transformPrefixesAndColl(resolvedPrefixes, quads)
  }

}