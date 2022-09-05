package com.github.propi.rdfrules.gui

import com.github.propi.rdfrules.gui.operations.common.CommonCache
import com.thoughtworks.binding.Binding.Var
import org.scalajs.dom.raw.FormData
import org.scalajs.dom.{BeforeUnloadEvent, window}

import java.util.UUID
import scala.concurrent.{Future, Promise}
import scala.scalajs.concurrent.JSExecutionContext.Implicits._
import scala.scalajs.js
import scala.scalajs.js.JSConverters.{JSRichIterableOnce, JSRichMap}
import scala.scalajs.js.JSON
import scala.util.{Failure, Success}

object AutoCaching {

  private val localStorageOnOffKey = "auto-caching"
  private val localStorageCacheKey = "auto-caches"

  val isOn: Var[Boolean] = Var(LocalStorage.get(localStorageOnOffKey).flatMap(_.toBooleanOption).getOrElse(true))

  def toggle(): Unit = {
    val newValue = !isOn.value
    LocalStorage.put(localStorageOnOffKey, newValue.toString)
    isOn.value = newValue
  }

  private sealed trait CacheId

  private case class AutoCacheId(id: String) extends CacheId {
    override def toString: String = id
  }

  private case class FixedCacheId(id: String) extends CacheId {
    override def toString: String = id
  }

  sealed trait AutoCachingSession {

    def tryCache(operation: Operation): Option[Operation]

    def tryRevalidate[T](operation: Operation)(f: Operation => T): T

    private[AutoCaching] def clearUnusedCaches(): Future[Boolean]

  }

  private class AutoCachingImpl extends AutoCachingSession {

    private val caches = collection.mutable.ArrayBuffer.empty[(String, CacheId)]
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

    def tryRevalidate[T](operation: Operation)(f: Operation => T): T = {
      //we save all transformation caches for auto revalidation
      operation match {
        case cache: CommonCache =>
          val pipelineContent = stringifyOperation(cache)
          caches.addOne(pipelineContent -> FixedCacheId(cache.getId.getOrElse("")))
          if (lastCaches.contains(pipelineContent)) {
            lastCaches.remove(pipelineContent)
            f(cache)
          } else {
            cache.revalidated(f(_))
          }
        case _ => f(operation)
      }
    }

    def tryCache(operation: Operation): Option[Operation] = {
      if (!hasIndex) {
        hasIndex = operation.info.targetStructure == OperationStructure.Index
      }
      if (
      //if operation is Cache => No cache
        operation.info.groups(OperationGroup.Caching) ||
          //if operation is Action => No cache
          operation.info.`type` == Operation.Type.Action ||
          //if next operation is Cache => No cache again
          operation.getNextOperation.exists(x => x.info.groups(OperationGroup.Caching))
      ) {
        None
      } else {
        val pipelineContent = stringifyOperation(operation)
        val res = lastCaches.get(pipelineContent) match {
          case Some(id) =>
            //if the previous pipeline has been cached in past we use the cache again
            //if (operation.info.targetStructure == OperationStructure.Dataset && hasIndex) {
            //if dataset is indexed we removed all dataset caches, they are no more needed
            /*None
          } else {*/
            //any other saved cache is reused
            lastCaches.remove(pipelineContent)
            cacheOperation(operation, id.toString).map(_ -> id)
          //}
          case None =>
            if (
            //index is always cached
              operation.info == OperationInfo.Loading.LoadIndex ||
                //index is always cached
                operation.info == OperationInfo.DatasetTransformation.Index ||
                //after mining it is cached
                operation.info == OperationInfo.IndexTransformation.Mine ||
                //last ruleset operation is cached (before action or transformation to other structure)
                (operation.info.targetStructure == OperationStructure.Ruleset && operation.getNextOperation.exists(x => x.info.isTransforming)) // ||
            //last dataset operation is cached (before action), cache is not created after ToDataset operation from index
            /*(operation.info.targetStructure == OperationStructure.Dataset &&
              Iterator.iterate(Option(operation))(_.flatMap(_.previousOperation.value)).takeWhile(_.isDefined).map(_.get).forall(_.info.sourceStructure != OperationStructure.Index) &&
              operation.getNextOperation.exists(x => x.info.`type` == Operation.Type.Action
            ))*/
            ) {
              val id = UUID.randomUUID().toString
              cacheOperation(operation, id).map(_ -> AutoCacheId(id))
            } else {
              None
            }
        }
        res.foreach(x => caches += (pipelineContent -> x._2))
        res.map(_._1)
      }
    }

    private[AutoCaching] def clearUnusedCaches(): Future[Boolean] = {
      Future.traverse(lastCaches.valuesIterator.filter(_.isInstanceOf[AutoCacheId]))(id => if (id.toString.isEmpty) Future.successful(true) else Endpoint.removeCache(id.toString)).map { it =>
        lastCaches.clear()
        lastCaches.addAll(caches)
        caches.clear()
        it.forall(x => x)
      }
    }

  }

  object Noop extends AutoCachingSession {
    def tryCache(operation: Operation): Option[Operation] = None

    def tryRevalidate[T](operation: Operation)(f: Operation => T): T = f(operation)

    private[AutoCaching] def clearUnusedCaches(): Future[Boolean] = Future.successful(true)
  }

  def apply[T](f: AutoCachingSession => T): Future[T] = {
    val autoCaching = if (isOn.value) new AutoCachingImpl else Noop
    val res = f(autoCaching)
    autoCaching.clearUnusedCaches().map(_ => res)
  }

  def saveCache(): Unit = if (isOn.value && lastCaches.nonEmpty) {
    LocalStorage.put(localStorageCacheKey, JSON.stringify(lastCaches.view.filter(_._2.isInstanceOf[AutoCacheId]).mapValues(_.toString).toMap.toJSDictionary))
  }

  private def saveAlias(id: String): Future[String] = {
    val alias = UUID.randomUUID().toString
    val formData = new FormData()
    formData.append("alias", alias)
    val result = Promise[String]()
    Endpoint.postWithAutoContentType[String](s"/cache/$id", formData) { response =>
      if (response.status == 200) {
        result.success(alias)
      } else {
        result.failure(new NoSuchElementException(response.data))
      }
    }
    result.future
  }

  def loadCache(): Future[Boolean] = if (isOn.value) {
    LocalStorage.get(localStorageCacheKey)
      .map(JSON.parse(_))
      .filter(js.typeOf(_) == "object")
      .iterator
      .flatMap(_.asInstanceOf[js.Dictionary[String]])
      .foldLeft(Future.successful(List.empty[(String, CacheId)])) { case (result, (content, id)) =>
        result.flatMap(list => saveAlias(id).map(alias => (content -> AutoCacheId(alias)) :: list).recover {
          case th: NoSuchElementException =>
            th.printStackTrace()
            list
        })
      }.transform {
      case Success(list) =>
        LocalStorage.remove(localStorageCacheKey)
        lastCaches ++= list
        Success(true)
      case Failure(th) =>
        LocalStorage.remove(localStorageCacheKey)
        th.printStackTrace()
        Success(false)
    }
  } else {
    Future.successful(true)
  }

  private val lastCaches = collection.mutable.Map.empty[String, CacheId]

  private def fetchRemovingCaches(): Set[String] = LocalStorage.get(Canvas.removingCachesKey).map(JSON.parse(_).asInstanceOf[js.Array[String]]).iterator.flatten.toSet

  window.addEventListener("beforeunload", (_: BeforeUnloadEvent) => {
    if (isOn.value && lastCaches.nonEmpty) {
      val newRemovingCaches = fetchRemovingCaches() ++ lastCaches.valuesIterator.collect {
        case AutoCacheId(id) => id
      }
      LocalStorage.put(Canvas.removingCachesKey, JSON.stringify(newRemovingCaches.toJSArray))
    }
  })

  window.setInterval(() => {
    val removingCaches = fetchRemovingCaches()
    for (removedCaches <- Future.traverse(removingCaches)(x => Endpoint.removeCache(x).filter(x => x).map(_ => x))) {
      if (removedCaches.nonEmpty) {
        val newRemovingCaches = removingCaches -- removedCaches
        if (newRemovingCaches.isEmpty) {
          LocalStorage.remove(Canvas.removingCachesKey)
        } else {
          LocalStorage.put(Canvas.removingCachesKey, JSON.stringify(newRemovingCaches.toJSArray))
        }
        println(s"removed caches: ${removedCaches.mkString(", ")}")
      }
    }
  }, 10000)

}