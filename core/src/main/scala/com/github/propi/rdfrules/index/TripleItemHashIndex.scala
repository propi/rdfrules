package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.{Prefix, Quad, Triple, TripleItem}
import com.github.propi.rdfrules.index.IndexCollections.{MutableHashMap, TypedCollectionsBuilder}
import com.github.propi.rdfrules.utils.{Debugger, ForEach}

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
sealed trait TripleItemHashIndex extends TripleItemIndex {

  protected val hmap: MutableHashMap[Int, TripleItem]
  protected val sameAs: MutableHashMap[TripleItem, Int]
  protected val prefixMap: MutableHashMap[String, String]

  def trim(): Unit = {
    hmap.trim()
    sameAs.trim()
    prefixMap.trim()
  }

  def size: Int = hmap.size

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
    .map(i => i -> hmap.get(i))
    .find(_._2.forall(_ == x))
    .map(x => x._1 -> x._2.nonEmpty)
    .get

  private def addPrefix(tripleItem: TripleItem): Unit = tripleItem match {
    case TripleItem.PrefixedUri(Prefix.Full(prefix, nameSpace), _) if !prefixMap.contains(prefix) => prefixMap.put(prefix, nameSpace)
    case _ =>
  }

  private def addTripleItem(tripleItem: TripleItem): Int = {
    addPrefix(tripleItem)
    if (!sameAs.contains(tripleItem)) {
      val (i, itemIsAdded) = getId(tripleItem)
      if (!itemIsAdded) hmap.put(i, tripleItem)
      i
    } else {
      sameAs(tripleItem)
    }
  }

  private def removeSameResources(): Unit = {
    for (idObject <- sameAs.iterator.map(getId).filter(_._2).map(_._1)) {
      //remove existed sameAs object
      hmap.remove(idObject)
      //if there are some holes after removing, we move all next related items by one step above
      Iterator.iterate(idObject + 1)(_ + 1).takeWhile(x => hmap.get(x).exists(_.hashCode() != x)).foreach { oldId =>
        val item = hmap(oldId)
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

  def getNamespace(prefix: String): Option[String] = prefixMap.get(prefix)

  private def resolvedTripleItem(x: TripleItem): TripleItem = x match {
    case x: TripleItem.PrefixedUri if x.prefix.prefix.nonEmpty && x.prefix.nameSpace.isEmpty =>
      getNamespace(x.prefix.prefix).map(Prefix(x.prefix.prefix, _)).map(prefix => x.copy(prefix = prefix)).getOrElse(x)
    case _ => x
  }

  def getIndexOpt(x: TripleItem): Option[Int] = {
    val resolved = resolvedTripleItem(x)
    sameAs.get(resolved).orElse(
      Iterator.iterate(resolved.hashCode())(_ + 1)
        .map(i => i -> hmap.get(i))
        .find(_._2.forall(_ == resolved))
        .flatMap(x => x._2.map(_ => x._1))
    ).orElse {
      Some(addTripleItem(resolved))
    }
  }

  def getTripleItemOpt(x: Int): Option[TripleItem] = hmap.get(x)

  def iterator: Iterator[(Int, TripleItem)] = hmap.pairIterator

  //def extendWith(ext: collection.Map[Int, TripleItem]): TripleItemIndex = new TripleItemHashIndex.ExtendedTripleItemHashIndex(hmap, sameAs, prefixMap, ext, () => trim())

  lazy val zero: Int = Iterator.from(Int.MinValue).find(getTripleItemOpt(_).isEmpty).get
}

object TripleItemHashIndex {

  /*class ExtendedTripleItemHashIndex private[TripleItemHashIndex](hmap: java.util.Map[Integer, TripleItem],
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
  }*/

  private class BasicTripleItemHashIndex(protected val hmap: MutableHashMap[Int, TripleItem],
                                         protected val sameAs: MutableHashMap[TripleItem, Int],
                                         protected val prefixMap: MutableHashMap[String, String]) extends TripleItemHashIndex

  private class JointTripleItemHashIndex(_hmap: MutableHashMap[Int, TripleItem],
                                         _sameAs: MutableHashMap[TripleItem, Int],
                                         _prefixMap: MutableHashMap[String, String],
                                         parent: TripleItemIndex) extends TripleItemHashIndex {
    protected val hmap: MutableHashMap[Int, TripleItem] = new SharedMutableHashMap[Int, TripleItem](_hmap) {
      def get(key: Int): Option[TripleItem] = parent.getTripleItemOpt(key).orElse(_hmap.get(key))

      def pairIterator: Iterator[(Int, TripleItem)] = parent.iterator ++ _hmap.pairIterator

      def size: Int = parent.size + _hmap.size
    }
    protected val sameAs: MutableHashMap[TripleItem, Int] = new SharedMutableHashMap[TripleItem, Int](_sameAs) {
      def get(key: TripleItem): Option[Int] = parent.getIndexOpt(key).filter(parent.getTripleItemOpt(_).exists(_ != key)).orElse(_sameAs.get(key))

      def pairIterator: Iterator[(TripleItem, Int)] = _sameAs.pairIterator

      def size: Int = _sameAs.size
    }
    protected val prefixMap: MutableHashMap[String, String] = new SharedMutableHashMap[String, String](_prefixMap) {
      def get(key: String): Option[String] = parent.getNamespace(key).orElse(_prefixMap.get(key))

      def pairIterator: Iterator[(String, String)] = _prefixMap.pairIterator

      def size: Int = _prefixMap.size
    }
  }

  private abstract class SharedMutableHashMap[K, V](protected val own: MutableHashMap[K, V]) extends MutableHashMap[K, V] {
    def apply(key: K): V = get(key).get

    def contains(x: K): Boolean = get(x).isDefined

    def getOrElseUpdate(key: K, default: => V): V = get(key) match {
      case Some(x) => x
      case None =>
        val value = default
        put(key, value)
        value
    }

    def remove(key: K): Unit = own.remove(key)

    def put(key: K, value: V): Unit = own.put(key, value)

    def clear(): Unit = own.clear()

    def trim(): Unit = own.trim()

    def valuesIterator: Iterator[V] = pairIterator.map(_._2)

    def iterator: Iterator[K] = pairIterator.map(_._1)

    def isEmpty: Boolean = size == 0
  }

  private def buildBasicMaps(implicit collectionsBuilder: TypedCollectionsBuilder[Int]) = {
    val hmap = collectionsBuilder.emptyHashMap[TripleItem]
    val smap = collectionsBuilder.emptyAnyHashMap[TripleItem, Int]
    val pmap = collectionsBuilder.emptyAnyHashMap[String, String]
    (hmap, smap, pmap)
  }

  def fromIndexedItem(col: ForEach[(Int, TripleItem)])(implicit debugger: Debugger, collectionsBuilder: TypedCollectionsBuilder[Int]): TripleItemHashIndex = {
    val (hmap, smap, pmap) = buildBasicMaps
    val tihi = new BasicTripleItemHashIndex(hmap, smap, pmap)
    debugger.debug("Triple items indexing", forced = true) { ad =>
      for (kv <- col) {
        tihi.addPrefix(kv._2)
        hmap.put(kv._1, kv._2)
        ad.done()
      }
      tihi.trim()
    }
    tihi
  }

  def mapQuads[T](col: ForEach[Quad], parent: Option[TripleItemIndex])(f: ForEach[IndexItem[Int]] => T)(implicit collectionsBuilder: TypedCollectionsBuilder[Int]): (TripleItemHashIndex, T) = {
    val (hmap, smap, pmap) = buildBasicMaps
    val tihi = parent match {
      case Some(parent) => new JointTripleItemHashIndex(hmap, smap, pmap, parent)
      case None => new BasicTripleItemHashIndex(hmap, smap, pmap)
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

  def mapQuads[T](col: ForEach[Quad], parent: TripleItemIndex)(f: ForEach[IndexItem[Int]] => T)(implicit collectionsBuilder: TypedCollectionsBuilder[Int]): (TripleItemHashIndex, T) = {
    mapQuads(col, Some(parent))(f)
  }

  def mapQuads[T](col: ForEach[Quad])(f: ForEach[IndexItem[Int]] => T)(implicit collectionsBuilder: TypedCollectionsBuilder[Int]): (TripleItemHashIndex, T) = {
    mapQuads(col, None)(f)
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

  def apply(col: ForEach[Quad], parent: Option[TripleItemIndex])(implicit debugger: Debugger, collectionsBuilder: TypedCollectionsBuilder[Int]): TripleItemHashIndex = {
    debugger.debug("Triple items indexing", forced = true) { ad =>
      val (tihi, _) = mapQuads(col.takeWhile(_ => !debugger.isInterrupted), parent) { col =>
        col.foreach(_ => ad.done())
      }
      if (debugger.isInterrupted) {
        debugger.logger.warn(s"The triple item indexing task has been interrupted. The loaded index may not be complete.")
      }
      tihi
    }
  }

  def apply(col: ForEach[Quad], parent: TripleItemIndex)(implicit debugger: Debugger, collectionsBuilder: TypedCollectionsBuilder[Int]): TripleItemHashIndex = {
    apply(col, Some(parent))
  }

  def apply(col: ForEach[Quad])(implicit debugger: Debugger, collectionsBuilder: TypedCollectionsBuilder[Int]): TripleItemHashIndex = {
    apply(col, None)
  }

}