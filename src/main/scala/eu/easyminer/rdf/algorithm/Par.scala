package eu.easyminer.rdf.algorithm

import java.util.concurrent.atomic.AtomicInteger

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

/**
  * Created by propan on 16. 4. 2017.
  */
class Par[A, B] private(it: Iterator[A], fMap: A => B, fReduce: (B, B) => B, fUntil: B => Boolean) {

  private object ResultLock

  private implicit val ec = ExecutionContext.global

  private val promisedResult: Promise[B] = Promise()
  private val counter = new AtomicInteger(0)
  private var result: Option[B] = None

  def map[T](f: A => T) = new Par[A, T](it, f, (_, x) => x, x => false)

  def reduce(f: (B, B) => B) = new Par(it, fMap, f, fUntil)

  def until(f: B => Boolean) = new Par(it, fMap, fReduce, f)

  def fire() = {
    if (it.isEmpty) Iterator.empty.next()
    val p = Runtime.getRuntime.availableProcessors()
    for (_ <- 0 until p) fireItem()
    Await.result(promisedResult.future, Duration.Inf)
  }

  private def reduceItem(x: B) = ResultLock.synchronized {
    result = result.map(y => fReduce(y, x)).orElse(Some(x))
    if (result.exists(fUntil) || counter.decrementAndGet() == 0) promisedResult.trySuccess(result.get)
  }

  private def fireItem(): Unit = {
    val item = it.synchronized {
      if (it.hasNext) {
        counter.incrementAndGet()
        Some(it.next())
      } else {
        None
      }
    }
    item.foreach { item =>
      val future = Future {
        fMap(item)
      }
      future.onComplete {
        case Success(x) => if (!promisedResult.isCompleted) {
          fireItem()
          reduceItem(x)
        }
        case Failure(th) =>
          counter.decrementAndGet()
          promisedResult.tryFailure(th)
      }
    }
  }

}

object Par {

  def apply[A](it: Iterator[A]): Par[A, A] = new Par(it, x => x, (_, x) => x, x => false)

}