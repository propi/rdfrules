package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.{Quad, Triple, TripleItem}

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
class TripleItemHashIndex private(map: collection.Map[Int, TripleItem], sameAs: collection.Map[TripleItem, Int]) {

  def getIndex(x: TripleItem): Int = getIndexOpt(x).get

  def getIndexOpt(x: TripleItem): Option[Int] = sameAs.get(x).orElse(
    Stream.iterate(x.hashCode())(_ + 1)
      .map(i => i -> map.get(i))
      .find(_._2.forall(_ == x))
      .flatMap(x => x._2.map(_ => x._1))
  )

  def getTripleItem(x: Int): TripleItem = map(x)

  def getTripleItemOpt(x: Int): Option[TripleItem] = map.get(x)

  def iterator: Iterator[(Int, TripleItem)] = map.iterator

}

object TripleItemHashIndex {

  def fromIndexedItem(col: Traversable[(Int, TripleItem)]): TripleItemHashIndex = {
    new TripleItemHashIndex(collection.mutable.HashMap.empty[Int, TripleItem] ++= col, Map.empty)
  }

  def apply(col: Traversable[Quad]): TripleItemHashIndex = {
    val sameAs = collection.mutable.HashMap.empty[TripleItem, Int]
    val map = collection.mutable.HashMap.empty[Int, TripleItem]

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
      .map(i => i -> map.get(i))
      .find(_._2.forall(_ == x))
      .map(x => x._1 -> x._2.nonEmpty)
      .get

    for (Quad(Triple(s, p, o), g) <- col) {
      if (p.hasSameUriAs("http://www.w3.org/2002/07/owl#sameAs")) {
        val (idSubject, subjectIsAdded) = getId(s)
        val (idObject, objectIsAdded) = getId(o)
        if (!subjectIsAdded) map += (idSubject -> s)
        sameAs += (o -> idSubject)
        if (objectIsAdded) map -= idObject
      } else {
        for (item <- List(s, p, o, g)) {
          if (!sameAs.contains(item)) {
            val (i, itemIsAdded) = getId(item)
            if (!itemIsAdded) map += (i -> item)
          }
        }
      }
    }

    new TripleItemHashIndex(map, sameAs)
  }

}