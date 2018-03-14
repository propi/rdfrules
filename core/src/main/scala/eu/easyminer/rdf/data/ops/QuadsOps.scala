package eu.easyminer.rdf.data.ops

import eu.easyminer.rdf.data.{Prefix, Quad, Triple, TripleItem}
import eu.easyminer.rdf.data.Quad.QuadTraversableView

import scala.util.Try

/**
  * Created by Vaclav Zeman on 16. 2. 2018.
  */
trait QuadsOps[Coll] {

  protected def transformQuads(col: Traversable[Quad]): Coll

  def quads: QuadTraversableView

  def prefixes: Traversable[Prefix] = quads.prefixes

  def addPrefixes(prefixes: Traversable[Prefix]): Coll = transformQuads(new Traversable[Quad] {
    def foreach[U](f: Quad => U): Unit = {
      val map = prefixes.view.map(x => x.nameSpace -> x.prefix).toMap

      def tryToPrefix(uri: TripleItem.Uri) = uri match {
        case x: TripleItem.LongUri =>
          Try(x.toPrefixedUri).map { prefixedUri =>
            map.get(prefixedUri.nameSpace).map(prefix => prefixedUri.copy(prefix = prefix)).getOrElse(x)
          }.getOrElse(x)
        case x: TripleItem.PrefixedUri =>
          map.get(x.nameSpace).map(prefix => x.copy(prefix = prefix)).getOrElse(x)
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
        f(quad.copy(triple = updatedTriple))
      }
    }
  })

}