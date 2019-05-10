package com.github.propi.rdfrules.utils.workers

import scala.concurrent.{Future, Promise}

/**
  * Created by Vaclav Zeman on 9. 5. 2019.
  */
trait ResultConsumer[T] {

  def finish(): Unit

  def acceptPartialResult(x: T): Unit

  def error(th: Throwable, workerId: Int): Unit

}

object ResultConsumer {

  trait FutureResultConsumer[T, R] extends ResultConsumer[T] {
    protected val promisedResult: Promise[R] = Promise()

    def result: Future[R] = promisedResult.future
  }

}