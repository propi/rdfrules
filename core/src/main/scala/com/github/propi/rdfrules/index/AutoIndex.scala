package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.data.{Dataset, TripleItem}
import com.github.propi.rdfrules.utils.Debugger

import java.io.{File, OutputStream}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap

class AutoIndex private(val tripleItemMap: TripleItemIndex)(implicit val debugger: Debugger) extends Index {
  private def unsupportedOperationWithIndex = throw new UnsupportedOperationException("Fact index is missing.")

  def tripleMap: TripleIndex[Int] = unsupportedOperationWithIndex

  def toDataset: Dataset = unsupportedOperationWithIndex

  def cache(os: => OutputStream): Unit = unsupportedOperationWithIndex

  def cache(file: File): Index = unsupportedOperationWithIndex

  def withDebugger(implicit debugger: Debugger): Index = new AutoIndex(tripleItemMap)(debugger)
}

object AutoIndex {

  private class AutoTripleItemIndex extends TripleItemIndex {
    private val tripleItemToIndexMap = TrieMap.empty[TripleItem, Int]
    private val indexToTripleItemMap = TrieMap.empty[Int, TripleItem]
    private val prefixMap = TrieMap.empty[String, String]
    private val counter = new AtomicInteger(0)

    val zero: Int = -1

    def getNamespace(prefix: String): Option[String] = prefixMap.get(prefix)

    def getIndexOpt(x: TripleItem): Option[Int] = tripleItemToIndexMap.get(x).orElse({
      val newIndex = counter.incrementAndGet()
      tripleItemToIndexMap.putIfAbsent(x, newIndex).orElse {
        indexToTripleItemMap.put(newIndex, x)
        x match {
          case TripleItem.PrefixedUri(prefix, _) => prefixMap.put(prefix.prefix, prefix.nameSpace)
          case _ =>
        }
        Some(newIndex)
      }
    })

    def getTripleItemOpt(x: Int): Option[TripleItem] = indexToTripleItemMap.get(x)

    def iterator: Iterator[(Int, TripleItem)] = indexToTripleItemMap.iterator
  }

  def apply(): Index = new AutoIndex(new AutoTripleItemIndex)

}