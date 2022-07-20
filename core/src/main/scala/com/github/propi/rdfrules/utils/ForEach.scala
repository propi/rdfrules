package com.github.propi.rdfrules.utils

import com.github.propi.rdfrules.utils.ForEach.{KnownSizeForEach, ParallelForEach}

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.{LinkedBlockingQueue, TimeUnit}
import scala.collection.immutable.ArraySeq
import scala.collection.{Factory, MapView, mutable}
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

trait ForEach[+T] {
  self =>

  def foreach(f: T => Unit): Unit

  /**
    * Do parallel mapping f in separated threads. New ForEach is traversing in same thread (no threads collision is happened - it is thread safe operation)
    *
    * @param parallelism     num of threads (default: availableProcessors)
    * @param maxWaitingTasks max waiting tasks in the buffer to be processed (default: 1024)
    * @param f               mapping function
    * @tparam A mapped type
    * @return
    */
  def parMap[A](parallelism: Int = Runtime.getRuntime.availableProcessors(), maxWaitingTasks: Int = 1024)(f: T => A): ForEach[A] = new ParallelForEach[T, A](self, parallelism, maxWaitingTasks)(f)

  def knownSize: Int = -1

  def withDebugger(dataLoadingText: String)(implicit debugger: Debugger): ForEach[T] = new ForEach[T] {
    def foreach(f: T => Unit): Unit = {
      val num = if (self.knownSize > 0) self.knownSize else 0
      debugger.debug(dataLoadingText, num) { ad =>
        for (x <- self.takeWhile(_ => !debugger.isInterrupted)) {
          f(x)
          ad.done()
        }
        if (debugger.isInterrupted) {
          debugger.logger.warn(s"The loading task has been interrupted. The loaded data may not be complete.")
        }
      }
    }

    override def knownSize: Int = self.knownSize
  }

  def topK[A >: T](k: Int, allowOverflowIfSameThreshold: Boolean = false)(updatedTail: A => Unit)(implicit ord: Ordering[A]): ForEach[A] = (f: A => Unit) => {
    val queue = new TopKQueue[A](k, allowOverflowIfSameThreshold)
    for (x <- self) {
      if (queue.isFull) {
        val enqueued = queue.enqueue(x)
        if (enqueued && !queue.isOverflowed) {
          queue.head.foreach(updatedTail)
        }
      } else {
        queue.enqueue(x)
      }
    }
    queue.dequeueAll.foreach(f)
  }

  def size: Int = if (knownSize >= 0) {
    knownSize
  } else {
    var i = 0
    foreach(_ => i += 1)
    i
  }

  def streamingCached[A >: T](implicit tag: ClassTag[A]): ForEach[A] = {
    var col: Either[ForEach[T], Array[A]] = Left(self)
    new ForEach[A] {
      override def knownSize: Int = col.fold(_.knownSize, _.knownSize)

      def foreach(f: A => Unit): Unit = col match {
        case Left(_col) =>
          val buffer = mutable.ArrayBuilder.make[A]
          for (x <- _col) {
            buffer.addOne(x)
            f(x)
          }
          col = Right(buffer.result())
        case Right(col) => col.foreach(f)
      }

      override def toIndexedSeq: IndexedSeq[A] = col.fold(_.toIndexedSeq, ArraySeq.unsafeWrapArray)

      override def toSeq: Seq[A] = toIndexedSeq

      override def toArray[B >: A](implicit tag: ClassTag[B]): Array[B] = col.fold(_.toArray, x => x.asInstanceOf[Array[B]])
    }
  }

  def cached[A >: T](implicit tag: ClassTag[A]): ForEach[A] = {
    lazy val col = self.to(Array)
    new ForEach[A] {
      override def knownSize: Int = col.knownSize

      def foreach(f: A => Unit): Unit = col.foreach(f)

      override def toIndexedSeq: IndexedSeq[A] = ArraySeq.unsafeWrapArray(col)

      override def toSeq: Seq[A] = toIndexedSeq

      override def toArray[B >: A](implicit tag: ClassTag[B]): Array[B] = col.asInstanceOf[Array[B]]
    }
  }

  def isEmpty: Boolean = headOption.isEmpty

  def lastOption: Option[T] = {
    val lastValue = MutableOption.empty[T]
    foreach(x => lastValue.set(x))
    lastValue.toOption
  }

  def headOption: Option[T] = {
    foreach(x => return Some(x))
    None
  }

  def distinct: ForEach[T] = (f: T => Unit) => {
    val set = collection.mutable.HashSet.empty[T]
    self.foreach { x =>
      if (!set(x)) {
        set += x
        f(x)
      }
    }
  }

  def distinctBy[A](f: T => A): ForEach[T] = (g: T => Unit) => {
    val set = collection.mutable.HashSet.empty[A]
    self.foreach { x =>
      val y = f(x)
      if (!set(y)) {
        set += y
        g(x)
      }
    }
  }

  def takeWhile(p: T => Boolean): ForEach[T] = new ForEach[T] {
    def foreach(f: T => Unit): Unit = {
      for (x <- self) {
        if (p(x)) {
          f(x)
        } else {
          return
        }
      }
    }
  }

  def take(n: Int): ForEach[T] = {
    if (n >= 0) {
      val col = new ForEach[T] {
        def foreach(f: T => Unit): Unit = {
          var i = 0
          self.foreach { x =>
            i += 1
            if (i <= n) f(x)
            if (i == n) return
          }
        }
      }
      if (knownSize >= 0) {
        new KnownSizeForEach(math.max(math.min(knownSize, n), 0), col)
      } else {
        col
      }
    } else {
      this
    }
  }

  def drop(n: Int): ForEach[T] = {
    if (n >= 0) {
      val col = new ForEach[T] {
        def foreach(f: T => Unit): Unit = {
          var i = 0
          self.foreach { x =>
            i += 1
            if (i > n) f(x)
          }
        }
      }
      if (knownSize >= 0) {
        new KnownSizeForEach(math.max(0, knownSize - n), col)
      } else {
        col
      }
    } else {
      this
    }
  }

  def head: T = headOption.get

  def last: T = lastOption.get

  def slice(from: Int, until: Int): ForEach[T] = drop(from).take(until - from)

  def groupedBy[K](g: T => K): ForEach[ForEach[T]] = (f: ForEach[T] => Unit) => {
    val hmap = collection.mutable.LinkedHashMap.empty[K, collection.mutable.ArrayBuffer[T]]
    for (x <- self) {
      val key = g(x)
      hmap.getOrElseUpdate(key, collection.mutable.ArrayBuffer.empty).addOne(x)
    }
    hmap.valuesIterator.map(ForEach.from).foreach(f)
  }

  def groupBy[K, C](g: T => K)(factory: Factory[T, C]): Map[K, C] = {
    val hmap = collection.mutable.Map.empty[K, collection.mutable.Builder[T, C]]
    self.foreach(x => hmap.getOrElseUpdate(g(x), factory.newBuilder).addOne(x))
    hmap.view.mapValues(_.result()).toMap
  }

  def concat[A >: T](that: ForEach[A]): ForEach[A] = {
    val col = new ForEach[A] {
      def foreach(f: A => Unit): Unit = {
        self.foreach(f)
        that.foreach(f)
      }
    }
    if (knownSize >= 0 && that.knownSize >= 0) {
      new KnownSizeForEach(knownSize + that.knownSize, col)
    } else {
      col
    }
  }

  def foldLeft[A](a: A)(f: (A, T) => A): A = {
    var res = a
    self.foreach(x => res = f(res, x))
    res
  }

  def flatMap[A](g: T => ForEach[A]): ForEach[A] = (f: A => Unit) => self.foreach(g(_).foreach(f))

  def reduce[A >: T](f: (A, A) => A): A = reduceOption(f).get

  def reduceOption[A >: T](f: (A, A) => A): Option[A] = foldLeft(Option.empty[A])((x, y) => x.map(f(y, _)).orElse(Some(y)))

  def map[A](g: T => A): ForEach[A] = {
    val col = new ForEach[A] {
      def foreach(f: A => Unit): Unit = self.foreach(x => f(g(x)))
    }
    if (knownSize >= 0) {
      new KnownSizeForEach(knownSize, col)
    } else {
      col
    }
  }

  def filter(p: T => Boolean): ForEach[T] = (f: T => Unit) => self.foreach(x => if (p(x)) f(x))

  def zipWithIndex: ForEach[(T, Int)] = {
    var i = 0
    map { x =>
      val y = x -> i
      i += 1
      y
    }
  }

  def find(p: T => Boolean): Option[T] = {
    self.foreach(x => if (p(x)) return Some(x))
    None
  }

  def exists(p: T => Boolean): Boolean = find(p).isDefined

  def forall(p: T => Boolean): Boolean = !exists(x => !p(x))

  def withFilter(p: T => Boolean): WithFilter = new WithFilter(p)

  def collect[A](g: PartialFunction[T, A]): ForEach[A] = (f: A => Unit) => self.foreach(x => if (g.isDefinedAt(x)) f(g(x)))

  def to[A >: T, C](factory: Factory[A, C]): C = {
    val x = factory.newBuilder
    foreach(y => x.addOne(y))
    x.result()
  }

  def toIndexedSeq: IndexedSeq[T] = to(IndexedSeq)

  def toSeq: Seq[T] = to(Seq)

  def toSet[A >: T]: Set[A] = to(Set)

  def toArray[A >: T](implicit tag: ClassTag[A]): Array[A] = to(Array)

  def histogram[A >: T]: MapView[A, Int] = {
    val map = collection.mutable.HashMap.empty[A, IncrementalInt]
    self.foreach(map.getOrElseUpdate(_, IncrementalInt()).++)
    map.view.mapValues(_.getValue)
  }

  class WithFilter(p: T => Boolean) {
    def map[B](f: T => B): ForEach[B] = self.filter(p).map(f)

    def flatMap[B](f: T => ForEach[B]): ForEach[B] = self.filter(p).flatMap(f)

    def foreach(f: T => Unit): Unit = self.filter(p).foreach(f)

    def withFilter(q: T => Boolean): WithFilter = new WithFilter(x => p(x) && q(x))
  }
}

object ForEach {

  private class ParallelForEach[T, A](col: ForEach[T], parallelism: Int, maxWaitingTasks: Int)(f: T => A) extends ForEach[A] {

    require(maxWaitingTasks >= 2)

    override def knownSize: Int = col.knownSize

    def foreach(g: A => Unit): Unit = {
      val executor = new java.util.concurrent.ForkJoinPool(parallelism)
      implicit val ec: ExecutionContext = ExecutionContext.fromExecutor(executor)
      val queue = new LinkedBlockingQueue[Option[A]](maxWaitingTasks)
      val isInterrupted = new AtomicBoolean(false)
      try {
        var i = 0
        val chain = col.foldLeft(Future.successful(Option.empty[A])) { (chain, x) =>
          val work = Future(Option(f(x)))
          val chained = chain.transformWith {
            case Success(res) =>
              res.foreach { x =>
                while (!queue.offer(Some(x), 100, TimeUnit.MILLISECONDS) && !isInterrupted.get()) ()
              }
              work
            case Failure(th) =>
              th.printStackTrace()
              queue.put(None)
              work
          }
          i += 1
          Option(queue.poll()).foreach { x =>
            x.foreach(g)
            i -= 1
          }
          if (i >= maxWaitingTasks) {
            queue.take().foreach(g)
            i -= 1
          }
          chained
        }
        while (i > 1) {
          queue.take().foreach(g)
          i -= 1
        }
        Try(Await.result(chain, Duration.Inf)).foreach(_.foreach(g))
      } finally {
        isInterrupted.set(true)
        executor.shutdown()
      }
    }
  }

  private class KnownSizeForEach[T](override val knownSize: Int, col: ForEach[T]) extends ForEach[T] {
    def foreach(f: T => Unit): Unit = col.foreach(f)
  }

  def empty[T]: ForEach[T] = from(Nil)

  //def apply[T](fe: (T => Unit) => Unit): ForEach[T] = (f: T => Unit) => fe(f)

  implicit def from[T](x: IterableOnce[T]): ForEach[T] = {
    val col = new ForEach[T] {
      def foreach(f: T => Unit): Unit = x.iterator.foreach(f)
    }
    if (x.knownSize >= 0) {
      new KnownSizeForEach(x.knownSize, col)
    } else {
      col
    }
  }

  def apply[T](x: T, xs: T*): ForEach[T] = {
    val col = new ForEach[T] {
      def foreach(f: T => Unit): Unit = {
        f(x)
        xs.foreach(f)
      }
    }
    new KnownSizeForEach[T](xs.length + 1, col)
  }

  implicit class PimpedDoubleForEach[T](val x: ForEach[ForEach[T]]) extends AnyVal {
    def flatten: ForEach[T] = x.flatMap(x => x)
  }

  implicit class PimpedTupleForEach[A, B](val x: ForEach[(A, B)]) extends AnyVal {
    def toMap: Map[A, B] = x.to(Map)
  }

}
