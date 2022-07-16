package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.{Prefix, Quad, Triple, TripleItem}
import com.github.propi.rdfrules.utils.{Debugger, ForEach}
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.{Object2IntOpenHashMap, Object2ObjectOpenHashMap}

import scala.jdk.CollectionConverters._

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
abstract class TripleItemHashIndex private(hmap: java.util.Map[Integer, TripleItem],
                                           sameAs: java.util.Map[TripleItem, Integer],
                                           prefixMap: java.util.Map[String, String]) extends TripleItemIndex {

  def trim(): Unit

  /**
    * Get id of a triple item.
    * If the triple item is added in map then it returns ID -> true
    * otherwise it returns ID -> false
    *
    * @param x triple item
    * @return
    */
  private def getId(x: TripleItem): (Int, Boolean) = Iterator
    .iterate(x.hashCode())(_ + 1)
    .map(i => i -> Option[TripleItem](hmap.get(i)))
    .find(_._2.forall(_ == x))
    .map(x => x._1 -> x._2.nonEmpty)
    .get

  private def addPrefix(tripleItem: TripleItem): Unit = tripleItem match {
    case TripleItem.PrefixedUri(Prefix.Full(prefix, nameSpace), _) if !prefixMap.containsKey(prefix) => prefixMap.put(prefix, nameSpace)
    case _ =>
  }

  private def addTripleItem(tripleItem: TripleItem): Int = {
    addPrefix(tripleItem)
    if (!sameAs.containsKey(tripleItem)) {
      val (i, itemIsAdded) = getId(tripleItem)
      if (!itemIsAdded) hmap.put(i, tripleItem)
      i
    } else {
      sameAs.get(tripleItem)
    }
  }

  private def removeSameResources(): Unit = {
    for (idObject <- sameAs.keySet().iterator().asScala.map(getId).filter(_._2).map(_._1)) {
      //remove existed sameAs object
      hmap.remove(idObject)
      //if there are some holes after removing, we move all next related items by one step above
      Iterator.iterate(idObject + 1)(_ + 1).takeWhile(x => Option[TripleItem](hmap.get(x)).exists(_.hashCode() != x)).foreach { oldId =>
        val item = hmap.get(oldId)
        hmap.remove(oldId)
        hmap.put(oldId - 1, item)
      }
    }
  }

  private def addQuad(quad: Quad): IndexItem[Int] = {
    val Quad(Triple(s, p, o), g) = quad
    if (p.hasSameUriAs(TripleItem.sameAs)) {
      List(s, o).foreach(addPrefix)
      val (idSubject, subjectIsAdded) = getId(s)
      if (!subjectIsAdded) hmap.put(idSubject, s)
      sameAs.put(o, idSubject)
      val (idObject, objectIsAdded) = getId(o)
      if (!objectIsAdded) hmap.put(idObject, o)
      IndexItem.SameAs(idSubject, idObject)
    } else {
      val res = Array(s, p, o, g).map(addTripleItem)
      IndexItem.Quad(res(0), res(1), res(2), res(3))
    }
  }

  def getNamespace(prefix: String): Option[String] = Option(prefixMap.get(prefix))

  private def resolvedTripleItem(x: TripleItem): TripleItem = x match {
    case x: TripleItem.PrefixedUri if x.prefix.prefix.nonEmpty && x.prefix.nameSpace.isEmpty =>
      getNamespace(x.prefix.prefix).map(Prefix(x.prefix.prefix, _)).map(prefix => x.copy(prefix = prefix)).getOrElse(x)
    case _ => x
  }

  def getIndexOpt(x: TripleItem): Option[Int] = {
    val resolved = resolvedTripleItem(x)
    Option(sameAs.get(resolved)).map(_.intValue()).orElse(
      Iterator.iterate(resolved.hashCode())(_ + 1)
        .map(i => i -> Option(hmap.get(i)))
        .find(_._2.forall(_ == resolved))
        .flatMap(x => x._2.map(_ => x._1))
    )
  }

  def getTripleItemOpt(x: Int): Option[TripleItem] = Option(hmap.get(x))

  def iterator: Iterator[(Int, TripleItem)] = hmap.entrySet().iterator().asScala.map(x => x.getKey.intValue() -> x.getValue)

  def extendWith(ext: collection.Map[Int, TripleItem]): TripleItemIndex = new TripleItemHashIndex.ExtendedTripleItemHashIndex(hmap, sameAs, prefixMap, ext, () => trim())
}

object TripleItemHashIndex {

  class ExtendedTripleItemHashIndex private[TripleItemHashIndex](hmap: java.util.Map[Integer, TripleItem],
                                                                 sameAs: java.util.Map[TripleItem, Integer],
                                                                 prefixMap: java.util.Map[String, String],
                                                                 ext1: collection.Map[Int, TripleItem],
                                                                 _trim: () => Unit) extends TripleItemHashIndex(hmap, sameAs, prefixMap) {
    private lazy val ext2: collection.Map[TripleItem, Int] = ext1.iterator.map(_.swap).toMap

    def trim(): Unit = _trim()

    override def getIndexOpt(x: TripleItem): Option[Int] = super.getIndexOpt(x).orElse(ext2.get(x))

    override def getTripleItemOpt(x: Int): Option[TripleItem] = super.getTripleItemOpt(x).orElse(ext1.get(x))

    override def iterator: Iterator[(Int, TripleItem)] = super.iterator ++ ext1.iterator

    override def extendWith(ext: collection.Map[Int, TripleItem]): TripleItemHashIndex = new ExtendedTripleItemHashIndex(hmap, sameAs, prefixMap, ext1 ++ ext, _trim)
  }

  private def buildBasicMaps = {
    val hmap = new Int2ObjectOpenHashMap[TripleItem]()
    val pmap = new Object2ObjectOpenHashMap[String, String]()
    hmap -> pmap
  }

  def fromIndexedItem(col: ForEach[(Int, TripleItem)])(implicit debugger: Debugger): TripleItemHashIndex = {
    val (hmap, pmap) = buildBasicMaps
    val tihi = new TripleItemHashIndex(hmap, new java.util.HashMap(), pmap) {
      def trim(): Unit = {
        hmap.trim()
        pmap.trim()
      }
    }
    debugger.debug("Triple items indexing") { ad =>
      for (kv <- col) {
        tihi.addPrefix(kv._2)
        hmap.put(kv._1, kv._2)
        ad.done()
      }
      tihi.trim()
    }
    tihi
  }

  def mapQuads[T](col: ForEach[Quad])(f: ForEach[IndexItem[Int]] => T): (TripleItemHashIndex, T) = {
    val sameAs = new Object2IntOpenHashMap[TripleItem]()
    val (hmap, pmap) = buildBasicMaps
    val tihi = new TripleItemHashIndex(hmap, sameAs, pmap) {
      def trim(): Unit = {
        hmap.trim()
        sameAs.trim()
        pmap.trim()
      }
    }
    tihi -> f(new ForEach[IndexItem[Int]] {
      def foreach(f: IndexItem[Int] => Unit): Unit = {
        try {
          for (quad <- col) {
            f(tihi.addQuad(quad))
          }
        } finally {
          tihi.removeSameResources()
          tihi.trim()
        }
      }

      override def knownSize: Int = col.knownSize
    })
  }

  def addQuads(col: ForEach[Quad])(implicit tihi: TripleItemHashIndex): ForEach[IndexItem[Int]] = {
    new ForEach[IndexItem[Int]] {
      def foreach(f: IndexItem[Int] => Unit): Unit = {
        try {
          for (quad <- col) {
            f(tihi.addQuad(quad))
          }
        } finally {
          tihi.removeSameResources()
        }
      }

      override def knownSize: Int = col.knownSize
    }
  }

  def apply(col: ForEach[Quad])(implicit debugger: Debugger): TripleItemHashIndex = {
    debugger.debug("Triple items indexing") { ad =>
      val (tihi, _) = mapQuads(col.takeWhile(_ => !debugger.isInterrupted)) { col =>
        col.foreach(_ => ad.done())
      }
      if (debugger.isInterrupted) {
        debugger.logger.warn(s"The triple item indexing task has been interrupted. The loaded index may not be complete.")
      }
      tihi
    }
  }

}