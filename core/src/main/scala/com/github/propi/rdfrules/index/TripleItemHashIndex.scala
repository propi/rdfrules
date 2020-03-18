package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.{Quad, Triple, TripleItem}
import com.github.propi.rdfrules.utils.Debugger
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.{Object2IntOpenHashMap, Object2ObjectOpenHashMap}

import scala.collection.JavaConverters._

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
class TripleItemHashIndex private(hmap: java.util.Map[Integer, TripleItem],
                                  sameAs: java.util.Map[TripleItem, Integer],
                                  prefixMap: java.util.Map[String, String]) {

  def getNamespace(prefix: String): Option[String] = Option(prefixMap.get(prefix))

  def getIndex(x: TripleItem): Int = getIndexOpt(x).get

  def getIndexOpt(x: TripleItem): Option[Int] = Option(sameAs.get(x)).map(_.intValue()).orElse(
    Stream.iterate(x.hashCode())(_ + 1)
      .map(i => i -> Option(hmap.get(i)))
      .find(_._2.forall(_ == x))
      .flatMap(x => x._2.map(_ => x._1))
  )

  def getTripleItem(x: Int): TripleItem = getTripleItemOpt(x).get

  def getTripleItemOpt(x: Int): Option[TripleItem] = Option(hmap.get(x))

  def iterator: Iterator[(Int, TripleItem)] = hmap.entrySet().iterator().asScala.map(x => x.getKey.intValue() -> x.getValue)

  def extendWith(ext: collection.Map[Int, TripleItem]): TripleItemHashIndex = new TripleItemHashIndex.ExtendedTripleItemHashIndex(hmap, sameAs, prefixMap, ext)

}

object TripleItemHashIndex {

  class ExtendedTripleItemHashIndex private[TripleItemHashIndex](hmap: java.util.Map[Integer, TripleItem],
                                                                 sameAs: java.util.Map[TripleItem, Integer],
                                                                 prefixMap: java.util.Map[String, String],
                                                                 ext1: collection.Map[Int, TripleItem]) extends TripleItemHashIndex(hmap, sameAs, prefixMap) {
    private lazy val ext2: collection.Map[TripleItem, Int] = ext1.iterator.map(_.swap).toMap

    override def getIndexOpt(x: TripleItem): Option[Int] = super.getIndexOpt(x).orElse(ext2.get(x))

    override def getTripleItemOpt(x: Int): Option[TripleItem] = super.getTripleItemOpt(x).orElse(ext1.get(x))

    override def iterator: Iterator[(Int, TripleItem)] = super.iterator ++ ext1.iterator

    override def extendWith(ext: collection.Map[Int, TripleItem]): TripleItemHashIndex = new ExtendedTripleItemHashIndex(hmap, sameAs, prefixMap, ext1 ++ ext)
  }

  def fromIndexedItem(col: Traversable[(Int, TripleItem)])(implicit debugger: Debugger): TripleItemHashIndex = {
    val hmap = new Int2ObjectOpenHashMap[TripleItem]()
    val pmap = new Object2ObjectOpenHashMap[String, String]()
    debugger.debug("Triple items indexing") { ad =>
      for (kv <- col) {
        kv._2 match {
          case TripleItem.PrefixedUri(prefix, nameSpace, _) if !pmap.containsKey(prefix) => pmap.put(prefix, nameSpace)
          case _ =>
        }
        hmap.put(kv._1, kv._2)
        ad.done()
      }
      hmap.trim()
      pmap.trim()
    }
    new TripleItemHashIndex(hmap, new java.util.HashMap(), pmap)
  }

  def apply(col: Traversable[Quad])(implicit debugger: Debugger): TripleItemHashIndex = {
    val sameAs = new Object2IntOpenHashMap[TripleItem]()
    val hmap = new Int2ObjectOpenHashMap[TripleItem]()
    val pmap = new Object2ObjectOpenHashMap[String, String]()

    /**
      * Get id of a triple item.
      * If the triple item is added in map then it returns ID -> true
      * otherwise it returns ID -> false
      *
      * @param x triple item
      * @return
      */
    def getId(x: TripleItem): (Int, Boolean) = Stream
      .iterate(x.hashCode())(_ + 1)
      .map(i => i -> Option[TripleItem](hmap.get(i)))
      .find(_._2.forall(_ == x))
      .map(x => x._1 -> x._2.nonEmpty)
      .get

    debugger.debug("Triple items indexing") { ad =>
      for (Quad(Triple(s, p, o), g) <- col) {
        for (TripleItem.PrefixedUri(prefix, nameSpace, _) <- List(s, p, o, g) if !pmap.containsKey(prefix)) {
          pmap.put(prefix, nameSpace)
        }
        if (p.hasSameUriAs("http://www.w3.org/2002/07/owl#sameAs")) {
          val (idSubject, subjectIsAdded) = getId(s)
          if (!subjectIsAdded) hmap.put(idSubject, s)
          sameAs.put(o, idSubject)
          val (idObject, objectIsAdded) = getId(o)
          if (objectIsAdded) {
            //remove existed sameAs object
            hmap.remove(idObject)
            //if there are some holes after removing, we move all next related items by one step above
            Stream.iterate(idObject + 1)(_ + 1).takeWhile(x => Option[TripleItem](hmap.get(x)).exists(_.hashCode() != x)).foreach { oldId =>
              val item = hmap.get(oldId)
              hmap.remove(oldId)
              hmap.put(oldId - 1, item)
            }
          }
        } else {
          for (item <- List(s, p, o, g)) {
            if (!sameAs.containsKey(item)) {
              val (i, itemIsAdded) = getId(item)
              if (!itemIsAdded) hmap.put(i, item)
            }
          }
        }
        ad.done()
      }
      hmap.trim()
      sameAs.trim()
      pmap.trim()
    }

    new TripleItemHashIndex(hmap, sameAs, pmap)
  }

}