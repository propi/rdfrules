package com.github.propi.rdfrules.utils

import com.typesafe.scalalogging.Logger

import scala.concurrent.duration.Duration

/**
  * Created by propan on 20. 3. 2017.
  */
trait HowLong {

  private val logger = Logger[HowLong]

  case class Stats(count: Int, totalTime: Duration, maxTime: Duration) {
    lazy val averageTime: Duration = Duration.fromNanos(totalTime.toNanos / count.toDouble)

    def +(time: Duration) = Stats(count + 1, totalTime + time, if (time > maxTime) time else maxTime)

    override def toString: String = s"(count: $count, totalTime: $totalTime, averageTime: $averageTime)"
  }

  private val times = collection.mutable.HashMap.empty[String, Stats]

  def howLong[T](message: String, silent: Boolean = false)(f: => T): T = if (logger.underlying.isDebugEnabled) {
    val time = System.nanoTime()
    val x = f
    val runningTime = Duration.fromNanos(System.nanoTime() - time)
    times.synchronized {
      times.update(message, times.getOrElse(message, Stats(0, Duration.Zero, Duration.Zero)) + runningTime)
    }
    if (!silent) logger.trace(message + ": " + runningTime)
    x
  } else {
    f
  }

  def flushAllResults(): Unit = {
    for ((message, stats) <- times) {
      logger.debug("TOTAL - " + message + ": " + stats)
    }
    times.clear()
  }

}

object HowLong extends HowLong
