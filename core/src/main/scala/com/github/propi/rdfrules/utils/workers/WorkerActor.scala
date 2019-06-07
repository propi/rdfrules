package com.github.propi.rdfrules.utils.workers

import java.util.concurrent.LinkedBlockingQueue

import com.github.propi.rdfrules.utils.workers.WorkerActor.Message
import com.github.propi.rdfrules.utils.workers.WorkerActor.Message.StoppingToken

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Random, Success, Try}

/**
  * Created by Vaclav Zeman on 9. 5. 2019.
  */
abstract class WorkerActor[I, O](val id: Int, resultConsumer: ResultConsumer[O])(implicit ec: ExecutionContext) extends Runnable {

  //private val logger = Logger(s"Worker $id")
  private val messages = new LinkedBlockingQueue[Message[I, O]]

  private def randomItem[T](x: IndexedSeq[T]): Option[T] = if (x.isEmpty) None else Some(x(Random.nextInt(x.length)))

  final def run(): Unit = {
    //collection for all registered workers
    val registeredWorkers = collection.mutable.ArrayBuffer.empty[WorkerActor[I, O]]
    //collection for all workers which this worker asked for work
    val askedWorkers = collection.mutable.Set.empty[Int]
    var stopping = false
    var stopped = false
    //all obtained stopping tokens during running phase
    val stoppingTokens = collection.mutable.Buffer.empty[Message.StoppingToken[I, O]]
    //RUNNING PHASE
    while (!stopping) {
      messages.take() match {
        //add new worker to registered workers
        case Message.RegisterWorker(x) =>
          //logger.info(s"register worker ${x.id}")
          if (x != this) registeredWorkers += x
        //this worked obtained piece of work, clear all asked workers and process this work
        case Message.Work(x) =>
          //logger.info(s"accepted work $x")
          askedWorkers.clear()
          doWork(x).onComplete(this ! Message.WorkCompleted(_))
        //work of this worker is completed, send result to consumer and then ask next worker for other work
        case Message.WorkCompleted(x) =>
          //logger.info(s"$id: completed work ${x.isSuccess}")
          x match {
            case Success(result) => resultConsumer.acceptPartialResult(result)
            case Failure(th) => resultConsumer.error(th, id)
          }
          randomItem(registeredWorkers) match {
            case Some(worker) => worker ! Message.AskForWork(this)
            case None => stopping = true
          }
        //some worker ask for work, if this worker is able to split its work then it sends piece of work to the sender
        //otherwise it sends no work
        case Message.AskForWork(sender) =>
          //logger.info(s"$id: ask for work from ${sender.id}")
          takePartOfWork match {
            case Some(x) => sender ! Message.Work(x)
            case None => sender ! Message.NoWork(this)
          }
        //if this worker obtains no work from the asked worker then ask other worker for work
        //or go to the stopping phase if all workers have no works
        case Message.NoWork(sender) =>
          //logger.info(s"$id: no work from ${sender.id}")
          val restWorkers = collection.mutable.ArrayBuffer.empty[WorkerActor[I, O]]
          for ((worker, i) <- registeredWorkers.iterator.zipWithIndex) {
            if (worker == sender) askedWorkers += i
            if (!askedWorkers(i)) restWorkers += worker
          }
          randomItem(restWorkers) match {
            case Some(worker) =>
              worker ! Message.AskForWork(this)
            case None =>
              stopping = true
          }
        //if this worker obtained a stopping token it means that some worker is stopping,
        //but this worker is still in running phase therefore the stopping token must not be processed
        //we save this stopping token and we perform it later in the stopping phase
        case st@Message.StoppingToken(_, _) =>
          //logger.info(s"$id: stopping token obrained in running state - $st")
          stoppingTokens += st
        //this is unexpected behavior to obtain stop message during running phase
        case Message.Stop() =>
          //logger.info(s"$id: stop obrained in running state!!!")
          stopping = true
          stopped = true
      }
    }
    //logger.info(s"$id: stopping phase...")

    /**
      * Function for obtained stopping token processing
      * If the obtained stopping token has greater ID than my ID then the token will be skipped
      * This worker can process only stopping token with lower ID than my ID
      *  - this is due to fire the finish method in consumer only once as soon as all workers are in the stopping phase
      * If workersToStop collection is empty then all workers are in stopping mode (they resent this stopping token),
      * Then fire finish method within the consumer, go to the stopped phase and send stop message to all workers.
      * Otherwise resend stopping token to another worker.
      *
      * @param stoppingToken obtained stopping token
      */
    def processStoppingToken(stoppingToken: StoppingToken[I, O]): Unit = {
      if (stoppingToken.senderId < id) {
        stoppingToken.workersToStop match {
          case nextWorker :: tail => nextWorker ! stoppingToken.copy(workersToStop = tail)
          case Nil =>
            //logger.info(s"$id: I am master and I terminates compution!")
            resultConsumer.finish()
            registeredWorkers.foreach(_ ! Message.Stop())
            stopped = true
        }
      }
    }

    //process all saved stopping tokens
    for (stoppingToken <- stoppingTokens) {
      if (!stopped) processStoppingToken(stoppingToken)
    }
    stoppingTokens.clear()
    //if this worker is not in the stoped phase then send my stopping token to some worker
    if (!stopped) {
      randomItem(registeredWorkers) match {
        case Some(worker) => worker ! Message.StoppingToken(id, (registeredWorkers - worker).toList)
        case None => this ! Message.StoppingToken(id - 1, Nil)
      }
    }
    //STOPPING PHASE
    while (!stopped) {
      messages.take() match {
        //if this worker is in the stopping phase then it refuses all asks for work
        case Message.AskForWork(sender) =>
          //logger.info(s"$id: Ask for work from ${sender.id} in stopping phase.")
          sender ! Message.NoWork(this)
        //process obtained stopping token
        case st@Message.StoppingToken(_, _) =>
          //logger.info(s"$id: stopping token obtained in stopping state - $st")
          processStoppingToken(st)
        //if this worker obtains stop message then go to the stopped phase immediately
        case Message.Stop() =>
          //logger.info(s"$id: stop obrained")
          stopped = true
        //other messages are ignored
        case _ =>
      }
    }
    //logger.info(s"$id: end of computing")
    //STOPPED PHASE - end of this thread
  }

  protected def doWork(x: I): Future[O]

  protected def takePartOfWork: Option[I]

  final def !(message: Message[I, O]): Unit = messages.put(message)

}

object WorkerActor {

  sealed trait Message[I, O]

  object Message {

    case class RegisterWorker[I, O] private[workers](workerActor: WorkerActor[I, O]) extends Message[I, O]

    case class AskForWork[I, O] private[workers](workerActor: WorkerActor[I, O]) extends Message[I, O]

    case class NoWork[I, O] private[workers](workerActor: WorkerActor[I, O]) extends Message[I, O]

    case class Work[I, O] private[workers](x: I) extends Message[I, O]

    case class WorkCompleted[I, O] private[workers](x: Try[O]) extends Message[I, O]

    case class StoppingToken[I, O](senderId: Int, workersToStop: List[WorkerActor[I, O]]) extends Message[I, O]

    case class Stop[I, O]() extends Message[I, O]

  }

}