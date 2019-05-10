package com.github.propi.rdfrules.utils.workers

import com.github.propi.rdfrules.utils.workers.ResultConsumer.FutureResultConsumer
import com.github.propi.rdfrules.utils.workers.WorkerActor.Message

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by Vaclav Zeman on 9. 5. 2019.
  */
object Workers {

  def startWorkers[T, I <: Seq[T], O](work: IndexedSeq[T], workers: IndexedSeq[WorkerActor[I, O]]): Unit = {
    //println(s"start parallel computing with ${workers.length} workers and with work size ${work.length}")
    workers.foreach(new Thread(_).start())
    for {
      worker <- workers
      workerToRegister <- workers if workerToRegister != worker
    } {
      worker ! Message.RegisterWorker(workerToRegister)
    }
    val it = work.iterator.grouped(math.ceil(work.length / workers.length.toDouble).toInt)
    for (worker <- workers) {
      if (it.hasNext) {
        worker ! Message.Work(it.next().asInstanceOf[I])
      } else {
        worker ! Message.Work(List.empty.asInstanceOf[I])
      }
    }
  }

  def apply[I](coll: IndexedSeq[I], parallelism: Int = Runtime.getRuntime.availableProcessors())(f: I => Unit)(implicit ec: ExecutionContext = ExecutionContext.global): Future[Unit] = {
    @volatile var workerError: Option[Throwable] = None
    val resultConsumer = new FutureResultConsumer[Unit, Unit] {
      def finish(): Unit = {
        workerError match {
          case Some(th) => promisedResult.failure(th)
          case None => promisedResult.success(Unit)
        }
      }

      def acceptPartialResult(x: Unit): Unit = {}

      def error(th: Throwable, workerId: Int): Unit = workerError = Some(th)
    }
    val workers: Iterator[WorkerActor[Seq[I], Unit]] = (0 until parallelism).iterator.map { id =>
      new WorkerActor[Seq[I], Unit](id, resultConsumer) {
        private val workQueue = collection.mutable.Queue.empty[I]

        protected def doWork(x: Seq[I]): Future[Unit] = {
          workQueue.enqueue(x: _*)
          Future {
            Stream.continually {
              val work = workQueue.synchronized {
                if (workQueue.nonEmpty) {
                  Some(workQueue.dequeue())
                } else {
                  None
                }
              }
              work.foreach(f)
              work.isDefined
            }.find(x => !x || workerError.isDefined)
            if (workerError.isDefined) workQueue.synchronized(workQueue.clear())
          }
        }

        protected def takePartOfWork: Option[Seq[I]] = workQueue.synchronized {
          if (workQueue.nonEmpty) {
            val queueLength = workQueue.length
            val buffer = collection.mutable.ListBuffer.empty[I]
            (0 until math.floor(queueLength / 2.0).toInt).foreach(_ => buffer += workQueue.dequeue())
            if (buffer.nonEmpty) Some(buffer.toList) else None
          } else {
            None
          }
        }
      }
    }
    startWorkers(coll, workers.toIndexedSeq)
    resultConsumer.result
  }

}