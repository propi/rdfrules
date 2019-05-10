package com.github.propi.rdfrules.utils

import java.util.Locale

import scala.concurrent.duration._
import scala.concurrent.duration.{Duration, FiniteDuration, TimeUnit}

/**
  * Created by Vaclav Zeman on 7. 5. 2019.
  */
object PrettyDuration {

  implicit class PimpedDuration(val duration: Duration) extends AnyVal {
    def pretty: String = pretty(includeNanos = false)

    def pretty(includeNanos: Boolean, precision: Int = 4): String = {
      require(precision > 0, "precision must be > 0")

      duration match {
        case d: FiniteDuration =>
          val nanos = d.toNanos
          val unit = chooseUnit(nanos)
          val value = nanos.toDouble / NANOSECONDS.convert(1, unit)

          s"%.${precision}g %s%s".formatLocal(
            Locale.ROOT,
            value,
            abbreviate(unit),
            if (includeNanos) s" ($nanos ns)" else "")

        case Duration.MinusInf => s"-∞ (minus infinity)"
        case Duration.Inf => s"∞ (infinity)"
        case _ => "undefined"
      }
    }

    private def chooseUnit(nanos: Long): TimeUnit = {
      val d = nanos.nanos

      if (d.toDays > 0) DAYS
      else if (d.toHours > 0) HOURS
      else if (d.toMinutes > 0) MINUTES
      else if (d.toSeconds > 0) SECONDS
      else if (d.toMillis > 0) MILLISECONDS
      else if (d.toMicros > 0) MICROSECONDS
      else NANOSECONDS
    }

    private def abbreviate(unit: TimeUnit): String = unit match {
      case NANOSECONDS => "ns"
      case MICROSECONDS => "μs"
      case MILLISECONDS => "ms"
      case SECONDS => "s"
      case MINUTES => "min"
      case HOURS => "h"
      case DAYS => "d"
    }
  }

}