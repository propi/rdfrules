package com.github.propi.rdfrules.utils

import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.Duration
import PrettyDuration._

/**
  * Created by propan on 20. 3. 2017.
  */
object HowLong {

  private val logger = Logger[HowLong.type]

  case class Stats(count: Int, totalTime: Duration, maxTime: Duration, memUsage: Long) {
    lazy val averageTime: Duration = Duration.fromNanos(totalTime.toNanos / count.toDouble)
    lazy val avarageMem: Double = memUsage / count.toDouble

    def +(time: Duration): Stats = Stats(count + 1, totalTime + time, if (time > maxTime) time else maxTime, memUsage)

    def +(timeMem: (Duration, Long)): Stats = (this + timeMem._1).copy(memUsage = memUsage + timeMem._2)

    override def toString: String = {
      val x = s"(count: $count, totalTime: ${totalTime.pretty}, maxTime: ${maxTime.pretty}, averageTime: ${averageTime.pretty}"
      if (memUsage != 0) {
        x + s", memUsage: ${memUsage / 1000000.0} MB)"
      } else {
        x + ")"
      }
    }
  }

  private val times = collection.mutable.HashMap.empty[String, Stats]

  private def getCurrentMemory = Runtime.getRuntime.totalMemory() - Runtime.getRuntime.freeMemory()

  def howLong[T](message: String, silent: Boolean = false, memUsage: Boolean = false, forceShow: Boolean = false)(f: => T): T = if (forceShow || logger.underlying.isDebugEnabled) {
    val beforeUsedMem = if (memUsage) {
      System.gc()
      getCurrentMemory
    } else 0
    val time = System.nanoTime()
    val x = f
    val runningTime = Duration.fromNanos(System.nanoTime() - time)
    val afterUsedMem = if (memUsage) {
      System.gc()
      getCurrentMemory
    } else 0

    times.synchronized {
      val stats = {
        val stats = times.getOrElse(message, Stats(0, Duration.Zero, Duration.Zero, 0))
        if (memUsage) {
          stats + (runningTime -> (afterUsedMem - beforeUsedMem))
        } else {
          stats + runningTime
        }
      }
      times.update(message, stats)
    }
    if (!silent) {
      logger.trace(message + ": " + runningTime.pretty)
      if (memUsage) logger.trace(message + ": " + ((afterUsedMem - beforeUsedMem) / 1000000.0) + " MB")
    }
    x
  } else {
    f
  }

  def get(message: String): Option[Stats] = times.get(message)

  def flushAllResults(): Unit = {
    for ((message, stats) <- times) {
      logger.info("TOTAL - " + message + ": " + stats)
    }
    times.clear()
  }

}