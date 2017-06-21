package eu.easyminer.rdf.utils

import scala.concurrent.duration.Duration

/**
  * Created by propan on 20. 3. 2017.
  */
trait HowLong {

  case class Stats(count: Int, totalTime: Duration) {
    lazy val averageTime = Duration.fromNanos(totalTime.toNanos / count.toDouble)

    override def toString: String = s"(count: $count, totalTime: $totalTime, averageTime: $averageTime)"
  }

  private val times = collection.mutable.HashMap.empty[String, Stats]

  def howLong[T](message: String, silent: Boolean = false)(f: => T): T = {
    val time = System.nanoTime()
    val x = f
    val runningTime = Duration.fromNanos(System.nanoTime() - time)
    times += (message -> times.get(message).map(x => Stats(x.count + 1, x.totalTime + runningTime)).getOrElse(Stats(1, runningTime)))
    if (!silent) println(message + ": " + runningTime)
    x
  }

  def flushAllResults() = {
    for ((message, stats) <- times) {
      println("TOTAL - " + message + ": " + stats)
    }
    times.clear()
  }

}

object HowLong extends HowLong
