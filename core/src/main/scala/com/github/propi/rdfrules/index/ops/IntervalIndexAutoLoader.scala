package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.data.TripleItem
import com.github.propi.rdfrules.index.{Index, IntervalsIndex}
import com.github.propi.rdfrules.utils.ForEach.from

trait IntervalIndexAutoLoader extends Index {

  lazy val intervalsIndex: IntervalsIndex = {
    this.main.debugger.debug("Intervals index loading", parts.map(_._2).map(_.tripleMap.predicates.size).sum * 2, true) { ad =>
      val AutoDiscretizationPattern = "(.*)#discretized_level_\\d+".r
      val index = parts.map(_._2).flatMap { indexPart =>
        val mapper = indexPart.tripleItemMap
        val pindex = indexPart.tripleMap.predicates
        val parentMap = pindex.iterator.flatMap { p =>
          val hasIntervals = pindex(p).objects.iterator.map(mapper.getTripleItem).collect {
            case x: TripleItem.Interval => x
          }.nonEmpty
          val res = if (hasIntervals) {
            mapper.getTripleItem(p) match {
              case pUri: TripleItem.Uri =>
                pUri.uri match {
                  case AutoDiscretizationPattern(parent) => Some(parent -> p)
                  case _ => Some(pUri.uri -> p)
                }
              case _ => None
            }
          } else {
            None
          }
          ad.done()
          res
        }.groupBy(_._1)(List)
        pindex.iterator.flatMap { p =>
          val res = mapper.getTripleItem(p) match {
            case pUri: TripleItem.Uri => parentMap.get(pUri.uri) match {
              case Some(children) => children.iterator.map(x => x._2 -> p)
              case None => Nil
            }
            case _ => Nil
          }
          ad.done()
          res
        }
      }.toMap
      new IntervalsIndex {
        def isEmpty: Boolean = index.isEmpty

        def parent(x: Int): Option[Int] = index.get(x)

        def iterator: Iterator[(Int, Int)] = index.iterator
      }
    }
  }

}