package com.github.propi.rdfrules.index.ops

import com.github.propi.rdfrules.data.{Prefix, TripleItem}
import com.github.propi.rdfrules.index.IndexCollections.TypedCollectionsBuilder
import com.github.propi.rdfrules.index.ops.Cacheable.{IndexPartType, Quad, SerItem, TripleItemInt}
import com.github.propi.rdfrules.index.ops.QuadsIndex.PimpedQuadsIndex
import com.github.propi.rdfrules.index._
import com.github.propi.rdfrules.serialization.IndexItemSerialization._
import com.github.propi.rdfrules.serialization.TripleItemSerialization._
import com.github.propi.rdfrules.utils.{Debugger, ForEach}
import com.github.propi.rdfrules.utils.serialization.Deserializer.Reader
import com.github.propi.rdfrules.utils.serialization.{Deserializer, Serializer}
import com.github.propi.rdfrules.serialization.IndexPartTypeSerialization._

import java.io.{InputStream, OutputStream}

/**
  * Created by Vaclav Zeman on 12. 3. 2018.
  */
trait Cacheable {

  self: Index =>

  def cache(os: () => OutputStream): Index = {
    Serializer.serializeToOutputStream[SerItem](os()) { writer =>
      val debugger = main.debugger
      debugger.debug("Triple items caching") { ad =>
        tripleItemMap.iterator.foreach(x => writer.write(TripleItemInt(x._2, x._1)))
        ad.done()
      }
      val numOfParts = parts.size
      for (((partType, index), num) <- parts.iterator.zipWithIndex) {
        writer.write(IndexPartType(partType))
        debugger.debug(s"Triples caching, stage ${num + 1} of $numOfParts") { ad =>
          index.compressedQuads.foreach { x =>
            writer.write(Quad(x))
            ad.done()
          }
        }
      }
    }
    self
  }

}

object Cacheable {

  private sealed trait SerItem

  private case class TripleItemInt(tripleItem: TripleItem, num: Int) extends SerItem

  private case class IndexPartType(partType: Index.PartType) extends SerItem

  private case class Quad(quad: IndexItem.IntQuad) extends SerItem

  implicit private val serItemSerializer: Serializer[SerItem] = {
    case TripleItemInt(tripleItem, num) => Serializer.directSerialize((0.toByte, tripleItem, num))
    case IndexPartType(partType) => Serializer.directSerialize(1.toByte -> partType)
    case Quad(quad) => Serializer.directSerialize(2.toByte -> quad)
  }

  implicit private val serItemDeserializer: Deserializer[SerItem] = (v: Array[Byte]) => v.headOption match {
    case Some(0) =>
      val res = Deserializer.directDeserialize[(Byte, TripleItem, Int)](v)
      TripleItemInt(res._2, res._3)
    case Some(1) =>
      IndexPartType(Deserializer.directDeserialize[(Byte, Index.PartType)](v)._2)
    case Some(2) =>
      Quad(Deserializer.directDeserialize[(Byte, IndexItem.IntQuad)](v)._2)
    case _ => throw new Deserializer.DeserializationException("Non parsable index ser item")
  }

  private def useInputStream[T](is: () => InputStream)(f: Iterator[SerItem] => T): T = {
    Deserializer.deserializeFromInputStream(is()) { reader: Reader[SerItem] =>
      f(Iterator.continually(reader.read()).takeWhile(_.isDefined).map(_.get))
    }
  }

  private def loadTripleItemIndexFromIt(it: Iterator[TripleItemInt])(implicit debugger: Debugger, collectionsBuilder: TypedCollectionsBuilder[Int]): TripleItemIndex = TripleItemHashIndex.fromIndexedItem((f: ((Int, TripleItem)) => Unit) => {
    val prefixes = collection.mutable.Map.empty[String, Prefix]
    for (TripleItemInt(tripleItem, num) <- it) {
      f(num -> (tripleItem match {
        case TripleItem.PrefixedUri(prefix, localName) =>
          val loadedPrefix = prefixes.getOrElseUpdate(prefix.nameSpace, prefix)
          if (loadedPrefix eq prefix) tripleItem else TripleItem.PrefixedUri(loadedPrefix, localName)
        case x => x
      }))
    }
  })

  def loadTripleItemIndex(is: () => InputStream)(implicit debugger: Debugger, collectionsBuilder: TypedCollectionsBuilder[Int]): TripleItemIndex = {
    useInputStream(is) { it =>
      loadTripleItemIndexFromIt(it.takeWhile(_.isInstanceOf[TripleItemInt]).map(_.asInstanceOf[TripleItemInt]))
    }
  }

  private def loadTripleIndexesFromIt(it: Iterator[SerItem])(implicit debugger: Debugger, collectionsBuilder: TypedCollectionsBuilder[Int]): Seq[(Index.PartType, TripleIndex[Int])] = {
    def parseHead(it: Iterator[SerItem], res: Seq[(Index.PartType, TripleIndex[Int])]): Seq[(Index.PartType, TripleIndex[Int])] = {
      if (it.hasNext) {
        it.next() match {
          case IndexPartType(partType) => parseBody(it, partType, res)
          case _ => res
        }
      } else {
        res
      }
    }

    def parseBody(it: Iterator[SerItem], partType: Index.PartType, res: Seq[(Index.PartType, TripleIndex[Int])]): Seq[(Index.PartType, TripleIndex[Int])] = {
      val (data, nextPart) = it.span(_.isInstanceOf[Quad])
      parseHead(nextPart, res :+ (partType -> TripleHashIndex(ForEach.from(data.map(_.asInstanceOf[Quad]).map(_.quad)))))
    }

    parseHead(it, Vector.empty)
  }

  def loadTripleIndexes(is: () => InputStream)(implicit debugger: Debugger, collectionsBuilder: TypedCollectionsBuilder[Int]): Seq[(Index.PartType, TripleIndex[Int])] = {
    useInputStream(is) { it =>
      loadTripleIndexesFromIt(it.filter(x => x.isInstanceOf[IndexPartType] || x.isInstanceOf[Quad]))
    }
  }

  def loadAll(is: () => InputStream)(implicit debugger: Debugger, collectionsBuilder: TypedCollectionsBuilder[Int]): (TripleItemIndex, Seq[(Index.PartType, TripleIndex[Int])]) = {
    useInputStream(is) { it =>
      val (mapping, data) = it.span(_.isInstanceOf[TripleItemInt])
      val tripleItemIndex = loadTripleItemIndexFromIt(mapping.map(_.asInstanceOf[TripleItemInt]))
      val parts = loadTripleIndexesFromIt(data)
      tripleItemIndex -> parts
    }
  }

}