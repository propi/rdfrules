package com.github.propi.rdfrules.index

import com.github.propi.rdfrules.algorithm.{RuleConsumer, RulesMining}
import com.github.propi.rdfrules.algorithm.consumer.InMemoryRuleConsumer
import com.github.propi.rdfrules.data.{Dataset, Quad, TripleItem}
import com.github.propi.rdfrules.index.TrainTestIndex.{QuadSerItem, SerItem, TripleItemIntSerItem}
import com.github.propi.rdfrules.index.ops.Cacheable
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.serialization.IndexItemSerialization._
import com.github.propi.rdfrules.serialization.TripleItemSerialization._
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}
import com.github.propi.rdfrules.utils.{Debugger, ForEach}

import java.io._
import scala.annotation.tailrec

sealed trait TrainTestIndex {
  def testIsTrain: Boolean

  def train: Index

  def test: Index

  def merged: Index

  def withDebugger(implicit debugger: Debugger): TrainTestIndex

  final def cache(os: => OutputStream): Unit = Serializer.serializeToOutputStream[SerItem](os) { writer =>
    train.debugger.debug("Triple items caching", test.tripleItemMap.size) { ad =>
      test.tripleItemMap.iterator.foreach { x =>
        writer.write(TripleItemIntSerItem(x._2, x._1))
        ad.done()
      }
    }
    train.debugger.debug("Triples caching", train.tripleMap.size(false)) { ad =>
      train.tripleMap.quads.foreach { x =>
        writer.write(QuadSerItem(true, x))
        ad.done()
      }
    }
    if (!testIsTrain) {
      train.debugger.debug("Test triples caching", test.tripleMap.size(false)) { ad =>
        test.tripleMap.quads.foreach { x =>
          writer.write(QuadSerItem(false, x))
          ad.done()
        }
      }
    }
  }

  final def cache(file: File): TrainTestIndex = {
    cache(new FileOutputStream(file))
    this
  }

  final def cache(file: String): TrainTestIndex = cache(new File(file))

  final def mineRules(miner: RulesMining, ruleConsumer: RuleConsumer.Invoker[Ruleset] = RuleConsumer(InMemoryRuleConsumer())): Ruleset = {
    train.mineRules(miner, ruleConsumer).withIndex(this)
  }
}

object TrainTestIndex {

  private sealed trait SerItem

  private case class HeaderSerItem(testIsTrain: Boolean) extends SerItem

  private case class TripleItemIntSerItem(tripleItem: TripleItem, num: Int) extends SerItem

  private case class QuadSerItem(isTrain: Boolean, quad: IndexItem.IntQuad) extends SerItem

  private implicit val serItemSerializer: Serializer[SerItem] = {
    case HeaderSerItem(testIsTrain) => Serializer.directSerialize((0.toByte, testIsTrain))
    case TripleItemIntSerItem(tripleItem, num) => Serializer.directSerialize((1.toByte, num, tripleItem))
    case QuadSerItem(true, quad) => Serializer.directSerialize((2.toByte, quad))
    case QuadSerItem(false, quad) => Serializer.directSerialize((3.toByte, quad))
  }

  private implicit val serItemDeserializer: Deserializer[SerItem] = (v: Array[Byte]) => if (v.head == 0) {
    val x = Deserializer.directDeserialize[(Byte, Boolean)](v)
    HeaderSerItem(x._2)
  } else if (v.head == 1) {
    val x = Deserializer.directDeserialize[(Byte, Int, TripleItem)](v)
    TripleItemIntSerItem(x._3, x._2)
  } else {
    val x = Deserializer.directDeserialize[(Byte, IndexItem.IntQuad)](v)
    if (x._1 == 1) {
      QuadSerItem(true, x._2)
    } else {
      QuadSerItem(false, x._2)
    }
  }

  private class OneIndex(val train: Index) extends TrainTestIndex {
    def testIsTrain: Boolean = true

    def test: Index = train

    def merged: Index = train

    def withDebugger(implicit debugger: Debugger): TrainTestIndex = new OneIndex(train.withDebugger)
  }

  private class TwoIndexes(val train: Index, val test: Index, _merged: => Index) extends TrainTestIndex {
    lazy val merged: Index = _merged

    def testIsTrain: Boolean = false

    def withDebugger(implicit debugger: Debugger): TrainTestIndex = new TwoIndexes(train.withDebugger, test.withDebugger, merged.withDebugger)
  }

  def apply(train: Index): TrainTestIndex = new OneIndex(train)

  def apply(train: Index, test: Dataset)(implicit debugger: Debugger): TrainTestIndex = {
    val testIndex = Index(test, train, false)
    new TwoIndexes(train, testIndex, Index(MergedTripleIndex(train.tripleMap, testIndex.tripleMap), testIndex.tripleItemMap))
  }

  private def trainTestSerItemToIndexSerItem(is: InputStream)(f: SerItem => Array[Byte]): InputStream = new InputStream {
    private val buffer = collection.mutable.Queue.empty[Byte]

    @tailrec
    def read(): Int = {
      if (buffer.isEmpty) {
        Deserializer.deserializeOpt[SerItem](is) match {
          case Some(x) =>
            val bytes = f(x)
            if (bytes.isEmpty) {
              read()
            } else {
              buffer.enqueueAll(bytes)
              buffer.dequeue()
            }
          case None => -1
        }
      } else {
        buffer.dequeue()
      }
    }
  }

  def fromCache(is: => InputStream, partially: Boolean)(implicit _debugger: Debugger): TrainTestIndex = {
    val _is = is
    val testIsTrain = try {
      Deserializer.deserializeFromInputStream(is) { reader: Deserializer.Reader[SerItem] =>
        reader.read() match {
          case Some(HeaderSerItem(testIsTrain)) => testIsTrain
          case _ => true
        }
      }
    } finally {
      _is.close()
    }
    val train = Index.fromCache(trainTestSerItemToIndexSerItem(is) {
      case TripleItemIntSerItem(tripleItem, num) => Serializer.serialize[Cacheable.SerItem](Left(num -> tripleItem))
      case QuadSerItem(true, quad) => Serializer.serialize[Cacheable.SerItem](Right(quad))
      case _ => Array.empty
    }, partially)
    if (testIsTrain) {
      val test = Dataset(new ForEach[Quad] {
        def foreach(f: Quad => Unit): Unit = {
          implicit val mapper: TripleItemIndex = train.tripleItemMap
          Deserializer.deserializeFromInputStream(is) { reader: Deserializer.Reader[SerItem] =>
            Iterator.continually(reader.read()).takeWhile(_.isDefined).map(_.get).collect {
              case QuadSerItem(false, quad) => quad.toQuad
            }.foreach(f)
          }
        }
      })
      apply(train, test)
    } else {
      apply(train)
    }
  }

  def fromCache(file: File, partially: Boolean)(implicit debugger: Debugger): TrainTestIndex = fromCache(new FileInputStream(file), partially)

  def fromCache(file: String, partially: Boolean)(implicit debugger: Debugger): TrainTestIndex = fromCache(new File(file), partially)
}