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
      operation.info.groups.collectFirst {
        case OperationGroup.Structure.Dataset => OperationInfo.DatasetTransformation.CacheDataset.buildOperation(operation, Some(cacheId))
        case OperationGroup.Structure.Index => OperationInfo.IndexTransformation.CacheIndex.buildOperation(operation, Some(cacheId))
        case OperationGroup.Structure.Ruleset => OperationInfo.RulesetTransformation.CacheRuleset.buildOperation(operation, Some(cacheId))
      }
    }

    def tryCache(operation: Operation): Option[Operation] = {
      if (!hasIndex) {
        hasIndex = operation.info.groups(OperationGroup.Structure.Index)
      }
      if (
        operation.info.groups(OperationGroup.Caching) ||
          operation.info.groups(OperationGroup.Structure.Model) ||
          operation.info.`type` == Operation.Type.Action ||
          operation.getNextOperation.exists(_.info.groups(OperationGroup.Caching))
      ) {
        None
      } else {
        val pipelineContent = stringifyOperation(operation)
        val res = lastCaches.get(pipelineContent) match {
          case Some(id) =>
            if (operation.info.groups(OperationGroup.Structure.Dataset) && hasIndex) {
              None
            } else {
              lastCaches.remove(pipelineContent)
              cacheOperation(operation, id).map(_ -> id)
            }
          case None =>
            if (
              operation.info == OperationInfo.IndexTransformation.LoadIndex ||
                operation.info == OperationInfo.DatasetTransformation.Index ||
                operation.info == OperationInfo.IndexTransformation.Mine ||
                (operation.info.groups(OperationGroup.Structure.Ruleset) && operation.getNextOperation.exists(x => x.info.`type` == Operation.Type.Action || x.info.groups(OperationGroup.Transforming))) ||
                (operation.info.groups(OperationGroup.Structure.Dataset) && operation.getNextOperation.exists(x => x.info.`type` == Operation.Type.Action))
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
