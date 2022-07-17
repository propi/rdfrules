package com.github.propi.rdfrules.utils.workers

import com.github.propi.rdfrules.utils.workers.ResultConsumer.FutureResultConsumer
import com.github.propi.rdfrules.utils.workers.WorkerActor.Message

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

//TODO replace Future by new Thread

/**
  * Created by Vaclav Zeman on 9. 5. 2019.
  *
  * This object is helper for starting multithreading computing with Workers (actors)
  * It better scales than the standard scala parallel collection.
  */
@deprecated("Use ForEach parMap")
object Workers {

  /**
    * Worker actor for some sequence of work.
    * It has a queue with work and is able to split work to two parts
    */
  private abstract class SeqWorkerActor[I, O](id: Int, resultConsumer: ResultConsumer[O])(implicit ec: ExecutionContext) extends WorkerActor[Seq[I], O](id, resultConsumer) {
    protected val workQueue: collection.mutable.Queue[I] = collection.mutable.Queue.empty

    /**
      * Split work to two parts and return one of them.
      *
      * @return splitted work, or None if the work can not be splitted
      */
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

  /**
    * Split some sequence of work to several parts,
    * start all defined workers,
    * distribute splitted work to each worker
    * and start the computing.
    *
    * @param work    sequence of work
    * @param workers list of workers
    * @tparam T one work item type
    * @tparam I the type of work sequence which each actor is able to process
    * @tparam O the result type of a worker
    */
  def startWorkers[T, I <: Seq[T], O](work: IndexedSeq[T], workers: IndexedSeq[WorkerActor[I, O]]): Unit = {
    //first start all workers threads
    workers.foreach(new Thread(_).start())
    //then register all workers
    for {
      worker <- workers
      workerToRegister <- workers if workerToRegister != worker
    } {
      worker ! Message.RegisterWorker(workerToRegister)
    }
    //split works to smaller chunks
    val it = if (work.isEmpty) Iterator.empty else work.iterator.grouped(math.ceil(work.length / workers.length.toDouble).toInt)
    //send splitted work to each worker
    for (worker <- workers) {
      if (it.hasNext) {
        worker ! Message.Work(it.next().asInstanceOf[I])
      } else {
        worker ! Message.Work(List.empty.asInstanceOf[I])
      }
    }
  }

  /**
    * Any sequence can be processed in parallel with Worker actors
    *
    * @param coll some indexed sequence
    * @tparam I the type of an item in the sequence
    */
  implicit class PimpedIndexedSeq[I](coll: IndexedSeq[I]) {

    /**
      * Parallel processing of each item of the sequence
      *
      * @param parallelism number of workers (it is number of available cores by default)
      * @param f           a function for processing a single item of the input sequence
      * @param ec          execution context for parallel processing
      */
    @deprecated("Use ForEach parMap")
    def parForeach(parallelism: Int = Runtime.getRuntime.availableProcessors())(f: I => Unit)(implicit ec: ExecutionContext = ExecutionContext.global): Unit = {
      //shared error variable; it is populated as soon as some worker ends with an exception
      //once the workerError variable is defined then all workers should stop computing
      @volatile var workerError: Option[Throwable] = None
      //the resultConsumer is able to process a worker error and set a result as soon as the parallel computing has been completed
      val resultConsumer = new FutureResultConsumer[Unit, Unit] {
        def finish(): Unit = {
          workerError match {
            case Some(th) => promisedResult.failure(th)
            case None => promisedResult.success(())
          }
        }

        def acceptPartialResult(x: Unit): Unit = {}

        def error(th: Throwable, workerId: Int): Unit = workerError = Some(th)
      }
      //create workers according to the parallelism level
      val workers: Iterator[WorkerActor[Seq[I], Unit]] = (0 until parallelism).iterator.map { id =>
        new SeqWorkerActor[I, Unit](id, resultConsumer) {
          protected def doWork(x: Seq[I]): Future[Unit] = {
            //an obtained work is saved into the mutable queue
            workQueue.enqueueAll(x)
            Future {
              //continually we dequeue the work collection and we apply a function to process the piece of work
              //we process the collection while the queue is not empty and there is no error during the computation
              Iterator.continually {
                //take out some piece of work from collection
                //it must be synchronized because the work can be splitted concurrently by another worker
                val work = workQueue.synchronized {
                  if (workQueue.nonEmpty) {
                    Some(workQueue.dequeue())
                  } else {
                    None
                  }
                }
                //process the piece of work
                work.foreach(f)
                work.isDefined
              }.find(x => !x || workerError.isDefined)
              //if some error in this worker or in another then clear all works (it is needed for stopping all workers immediatelly)
              if (workerError.isDefined) workQueue.synchronized(workQueue.clear())
            }
          }
        }
      }
      //we start all created workers
      startWorkers(coll, workers.toIndexedSeq)
      //wait for result
      Await.result(resultConsumer.result, Duration.Inf)
    }

    /**
      * Parallel mapping of each item of the sequence to another one
      *
      * @param parallelism number of workers (it is number of available cores by default)
      * @param f           a function for mapping a single item of the input sequence to another one
      * @param ec          execution context for parallel processing
      * @tparam O the type of mapped item
      * @return mapped collection
      */
    @deprecated("Use ForEach parMap")
    def parMap[O](parallelism: Int = Runtime.getRuntime.availableProcessors())(f: I => O)(implicit ec: ExecutionContext = ExecutionContext.global): IndexedSeq[O] = {
      val resultBuffer = collection.mutable.ArrayBuffer.empty[O]
      parForeach(parallelism) { x =>
        val y = f(x)
        resultBuffer.synchronized(resultBuffer += y)
      }
      resultBuffer.toIndexedSeq
    }

    /**
      * Parallel processing, mapping and enumeration of topK items from the sequence based on a threshold
      *
      * @param k               top k items to be returned
      * @param initThreshold   initial threshold value
      * @param updateThreshold function for convering an item (in the head of priority queue) to a threshold value for updating the min/max threshold
      * @param parallelism     number of workers (it is number of available cores by default)
      * @param growing         if true and the queue is full and new object is added with same threshold, the new one is added and older stays in the queue.
      *                        The result can contain more items than K.
      * @param f               a function for mapping a single item with threshold of the input sequence to another one
      * @param ordering        ordering of sequence items for topK enumeration
      * @param ec              execution context for parallel processing
      * @tparam O the type of mapped item
      * @tparam T the type of threshold
      * @return topK collection
      */
    def topK[O, T](k: Int, initThreshold: T, updateThreshold: O => T, parallelism: Int = Runtime.getRuntime.availableProcessors(), growing: Boolean = false)(f: (I, T) => Iterator[O])(implicit ordering: Ordering[O], ec: ExecutionContext = ExecutionContext.global): IndexedSeq[O] = {
      val normK = if (k <= 0) 1 else k
      //the shared threshold variable which is updating once the topK queue is full and the head is changed
      @volatile var threshold: T = initThreshold
      val longTail = collection.mutable.ListBuffer.empty[O]
      //priority queue
      val queue = collection.mutable.PriorityQueue.empty[O]
      parForeach(parallelism) { x =>
        //process the item with threshold
        for (y <- f(x, threshold)) {
          //update queue
          queue.synchronized {
            if (queue.length >= normK) {
              //if the queue is full and the newly processed item is better than the head of queue then
              // - dequeue the head
              // - enqueue the new item
              // - update threshold with a new head item of queue
              if (ordering.gt(queue.head, y)) {
                val dequeued = queue.dequeue()
                queue.enqueue(y)
                if (growing) {
                  if (ordering.equiv(queue.head, dequeued)) {
                    longTail += dequeued
                  } else {
                    longTail.clear()
                  }
                }
                threshold = updateThreshold(queue.head)
              } else if (growing && ordering.equiv(queue.head, y)) {
                longTail += y
              }
            } else {
              //if the queue is not full we enqueue the item
              queue.enqueue(y)
              //after 'enqueue' if the queue is full we update the threshold by the head queue item
              if (queue.length == normK) threshold = updateThreshold(queue.head)
            }
          }
        }
      }
      for (x <- longTail) {
        queue.enqueue(x)
      }
      longTail.clear()
      queue.dequeueAll.asInstanceOf[IndexedSeq[O]]
    }

  }

}