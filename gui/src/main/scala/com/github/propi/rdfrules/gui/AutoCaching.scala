package com.github.propi.rdfrules.gui

import com.thoughtworks.binding.Binding.Var

import java.util.UUID
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits._
import scala.scalajs.js
import scala.scalajs.js.JSON

object AutoCaching {

  private val localStorageKey = "auto-caching"

  val isOn: Var[Boolean] = Var(LocalStorage.get[Boolean](localStorageKey)(_.toBooleanOption).getOrElse(true))

  def toggle(): Unit = {
    val newValue = !isOn.value
    LocalStorage.put[Boolean](localStorageKey, newValue)(_.toString)
    isOn.value = newValue
  }

  sealed trait AutoCachingSession {

    def tryCache(operation: Operation): Option[Operation]

    private[AutoCaching] def clearUnusedCaches(): Future[Boolean]

  }

  private class AutoCachingImpl extends AutoCachingSession {

    private val caches = collection.mutable.ArrayBuffer.empty[(String, String)]
    private var hasIndex = false

    private def stringifyOperation(operation: Operation): String = JSON.stringify(js.Array(operation.toJson(Nil): _*))

    private def cacheOperation(operation: Operation, cacheId: String): Option[Operation] = {
      operation.info.targetStructure match {
        case OperationStructure.Dataset => Some(OperationInfo.DatasetTransformation.CacheDataset.buildOperation(operation, Some(cacheId)))
        case OperationStructure.Index => Some(OperationInfo.IndexTransformation.CacheIndex.buildOperation(operation, Some(cacheId)))
        case OperationStructure.Ruleset => Some(OperationInfo.RulesetTransformation.CacheRuleset.buildOperation(operation, Some(cacheId)))
        case _ => None
      }
    }

    def tryCache(operation: Operation): Option[Operation] = {
      if (!hasIndex) {
        hasIndex = operation.info.targetStructure == OperationStructure.Index
      }
      if (
        operation.info.groups(OperationGroup.Caching) || //if operation is Cache => No cache
          operation.info.`type` == Operation.Type.Action || //if operation is Action => No cache
          operation.getNextOperation.exists(_.info.groups(OperationGroup.Caching)) //if next operation is Cache => No cache again
      ) {
        None
      } else {
        val pipelineContent = stringifyOperation(operation)
        val res = lastCaches.get(pipelineContent) match {
          case Some(id) =>
            //if the previous pipeline has been cached in past we use the cache again
            if (operation.info.targetStructure == OperationStructure.Dataset && hasIndex) {
              //if dataset is indexed we removed all dataset caches, they are no more needed
              None
            } else {
              //any other saved cache is reused
              lastCaches.remove(pipelineContent)
              cacheOperation(operation, id).map(_ -> id)
            }
          case None =>
            if (
            //index is always cached
              operation.info == OperationInfo.Loading.LoadIndex ||
                //index is always cached
                operation.info == OperationInfo.DatasetTransformation.Index ||
                //after mining it is cached
                operation.info == OperationInfo.IndexTransformation.Mine ||
                //last ruleset operation is cached (before action or transformation to other structure)
                (operation.info.targetStructure == OperationStructure.Ruleset && operation.getNextOperation.exists(x => x.info.isTransforming)) ||
                //last dataset operation is cached (before action)
                (operation.info.targetStructure == OperationStructure.Dataset && operation.getNextOperation.exists(x => x.info.`type` == Operation.Type.Action))
            ) {
              val id = UUID.randomUUID().toString
              cacheOperation(operation, id).map(_ -> id)
            } else {
              None
            }
        }
        res.foreach(x => caches += (pipelineContent -> x._2))
        res.map(_._1)
      }
    }

    private[AutoCaching] def clearUnusedCaches(): Future[Boolean] = {
      Future.traverse(lastCaches.valuesIterator)(id => Endpoint.removeCache(id)).map { it =>
        lastCaches.clear()
        lastCaches.addAll(caches)
        caches.clear()
        it.forall(x => x)
      }
    }

  }

  object Noop extends AutoCachingSession {
    def tryCache(operation: Operation): Option[Operation] = None

    private[AutoCaching] def clearUnusedCaches(): Future[Boolean] = Future.successful(true)
  }

  def apply[T](f: AutoCachingSession => T): Future[T] = {
    val autoCaching = if (isOn.value) new AutoCachingImpl else Noop
    val res = f(autoCaching)
    autoCaching.clearUnusedCaches().map(_ => res)
  }

  private val lastCaches = collection.mutable.Map.empty[String, String]

}
