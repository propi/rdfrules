package com.github.propi.rdfrules.http

import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import com.github.propi.rdfrules.http.util.Conf
import com.typesafe.scalalogging.Logger

import scala.collection.concurrent.TrieMap
import scala.collection.mutable
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.reflect.ClassTag

/**
  * Created by Vaclav Zeman on 26. 1. 2020.
  */
//TODO dont save interrupted tasks into the cache
object InMemoryCache {

  private val logger = Logger[InMemoryCache.type]

  case class MemoryInfo(total: Long, free: Long, itemsInCache: Int) {
    def used: Long = total - free
  }

  private class CacheValue(val data: Any, lastUpdate: AtomicLong) {
    def this(data: Any) = this(data, new AtomicLong(Instant.now().getEpochSecond))

    def lastModified: Instant = Instant.ofEpochSecond(lastUpdate.get())

    def lastModifiedSeconds: Long = lastUpdate.get()

    def refresh(): Unit = lastUpdate.set(Instant.now().getEpochSecond)

    def to[T](implicit tag: ClassTag[T]): Option[T] = data match {
      case t: T => Some(t)
      case _ => None
    }
  }

  private val maxCacheItemLifetime = Conf[Duration](Main.confPrefix + ".cache.max-item-lifetime").toOption.getOrElse(1 hour)

  private val lifeTimesQueue = mutable.PriorityQueue.empty[(Long, String)]
  private val hmap = TrieMap.empty[String, CacheValue]

  def autoCleaningActor: Behavior[Boolean] = Behaviors.setup { context =>
    context.setReceiveTimeout(5 minutes, true)
    Behaviors.receiveMessage { _ =>
      lifeTimesQueue.synchronized {
        while (lifeTimesQueue.headOption.exists(x => x._1 + maxCacheItemLifetime.toSeconds < Instant.now().getEpochSecond)) {
          val (keyTime, key) = lifeTimesQueue.dequeue()
          for (cacheValue <- hmap.get(key)) {
            val cacheValueTime = cacheValue.lastModifiedSeconds
            if (cacheValueTime == keyTime) {
              hmap.remove(key)
              logger.info(s"Some value with key '$key' was removed from the memory cache due to expiration. Number of items in the cache is: ${hmap.size}")
            } else {
              lifeTimesQueue.enqueue(cacheValueTime -> key)
            }
          }
        }
      }
      Behaviors.same
    }
  }

  def remove(key: String): Unit = hmap.remove(key)

  def get[T](key: String)(implicit tag: ClassTag[T]): Option[T] = hmap.get(key).flatMap { value =>
    value.refresh()
    value.to[T]
  }

  def put(key: String, value: Any): Unit = {
    val cacheValue = new CacheValue(value)
    hmap.put(key, cacheValue)
    lifeTimesQueue.synchronized {
      lifeTimesQueue.enqueue(cacheValue.lastModifiedSeconds -> key)
    }
    logger.info(s"Some value with key '$key' was pushed into the memory cache. Number of items in the cache is: ${hmap.size}")
  }

  def clear(): Unit = {
    hmap.clear()
    lifeTimesQueue.synchronized {
      lifeTimesQueue.clear()
    }
    logger.info(s"The memory cache was cleaned")
  }

  def getMemoryInfo: MemoryInfo = MemoryInfo(Runtime.getRuntime.totalMemory(), Runtime.getRuntime.freeMemory(), hmap.size)

}