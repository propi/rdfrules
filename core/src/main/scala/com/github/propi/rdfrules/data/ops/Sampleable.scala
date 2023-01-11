package com.github.propi.rdfrules.data.ops

import com.github.propi.rdfrules.data.ops.Sampleable.{AbsolutePart, Part, RelativePart}
import com.github.propi.rdfrules.utils.ForEach

import scala.reflect.ClassTag
import scala.util.Random

trait Sampleable[T, Coll] extends Transformable[T, Coll] {

  self: Coll =>

  protected def samplingDistributor: Option[T => Any] = None

  implicit protected def valueClassTag: ClassTag[T]

  private class Sampler(strategy: IndexedSeq[Array[T]] => Iterator[(Int, T)]) extends ForEach[(Int, T)] {
    def foreach(f: ((Int, T)) => Unit): Unit = {
      val cache: IndexedSeq[Array[T]] = samplingDistributor match {
        case Some(distributor) => Random.shuffle(coll.groupBy(distributor(_))(Array).valuesIterator.toIndexedSeq)
        case None => IndexedSeq(coll.toArray[T])
      }
      strategy(cache).foreach(f)
    }
  }

  private def normParts(parts: IndexedSeq[Part], n: Int) = parts.map {
    case RelativePart(ratio) => math.round(n * ratio)
    case AbsolutePart(max) => max
  }

  private def shufflingStrategy(parts: IndexedSeq[Part])(cache: IndexedSeq[Array[T]]): Iterator[(Int, T)] = {
    val absoluteParts = normParts(parts, cache.iterator.map(_.length).sum)
    val pointers = Array.fill(cache.length)(0)
    val partsCounter = Array.fill(parts.length)(0)
    Iterator.continually[Iterator[(Int, T)]] {
      for {
        (partMax, part) <- absoluteParts.iterator.zipWithIndex if partsCounter(part) < partMax
        i <- Iterator.range(0, cache.length)
        pointer = pointers(i)
        cacheArray = cache(i) if pointer < cacheArray.length && partsCounter(part) < partMax
      } yield {
        val swapIndex = Random.nextInt(cacheArray.length - pointer) + pointer
        val temp = cacheArray(swapIndex)
        cacheArray(swapIndex) = cacheArray(pointer)
        cacheArray(pointer) = temp
        pointers(i) += 1
        partsCounter(part) += 1
        part -> temp
      }
    }.takeWhile(_.iterator.hasNext).flatten
  }

  private def bootstrappingStrategy(parts: IndexedSeq[Part])(cache: IndexedSeq[Array[T]]): Iterator[(Int, T)] = {
    val absoluteParts = normParts(parts, cache.iterator.map(_.length).sum)
    val partsCounter = Array.fill(parts.length)(0)
    Iterator.continually[Iterator[(Int, T)]] {
      for {
        (partMax, part) <- absoluteParts.iterator.zipWithIndex if partsCounter(part) < partMax
        cacheArray <- cache.iterator if partsCounter(part) < partMax
      } yield {
        val x = cacheArray(Random.nextInt(cacheArray.length))
        partsCounter(part) += 1
        part -> x
      }
    }.takeWhile(_.iterator.hasNext).flatten
  }

  private def transformSample(i: Int)(implicit sampler: ForEach[(Int, T)]) = transform(sampler.filter(_._1 == i).map(_._2))

  def shuffle: Coll = transform(new Sampler(shufflingStrategy(IndexedSeq(RelativePart(1.0f)))).map(_._2).streamingCached)

  def shuffle(part: Part): Coll = transform(new Sampler(shufflingStrategy(IndexedSeq(part))).map(_._2).streamingCached)

  def shuffle(train: Part, test: Part): (Coll, Coll) = {
    implicit val sampler: ForEach[(Int, T)] = new Sampler(shufflingStrategy(IndexedSeq(train, test))).streamingCached
    transformSample(0) -> transformSample(1)
  }

  def shuffle(train: Part, test: Part, valid: Part): (Coll, Coll, Coll) = {
    implicit val sampler: ForEach[(Int, T)] = new Sampler(shufflingStrategy(IndexedSeq(train, test, valid))).streamingCached
    (transformSample(0), transformSample(1), transformSample(2))
  }

  def bootstrap: Coll = transform(new Sampler(bootstrappingStrategy(IndexedSeq(RelativePart(1.0f)))).map(_._2).streamingCached)

  def bootstrap(part: Part): Coll = transform(new Sampler(bootstrappingStrategy(IndexedSeq(part))).map(_._2).streamingCached)

  def bootstrap(train: Part, test: Part): (Coll, Coll) = {
    implicit val sampler: ForEach[(Int, T)] = new Sampler(bootstrappingStrategy(IndexedSeq(train, test))).streamingCached
    transformSample(0) -> transformSample(1)
  }

  def bootstrap(train: Part, test: Part, valid: Part): (Coll, Coll, Coll) = {
    implicit val sampler: ForEach[(Int, T)] = new Sampler(bootstrappingStrategy(IndexedSeq(train, test, valid))).streamingCached
    (transformSample(0), transformSample(1), transformSample(2))
  }

}

object Sampleable {
  sealed trait Part

  case class RelativePart(ratio: Float) extends Part

  case class AbsolutePart(max: Int) extends Part
}