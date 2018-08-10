package com.github.propi.rdfrules.utils

import com.typesafe.scalalogging
import org.slf4j.event.Level
import org.slf4j.helpers.MessageFormatter
import org.slf4j.{Logger, LoggerFactory, Marker}

/**
  * Created by Vaclav Zeman on 9. 4. 2018.
  */
class CustomLogger(logger: Logger)(log: (String, Level) => Unit) extends Logger {
  def getName: String = logger.getName

  def isTraceEnabled: Boolean = logger.isTraceEnabled

  def trace(msg: String): Unit = {
    log(msg, Level.TRACE)
    logger.trace(msg)
  }

  def trace(format: String, arg: scala.Any): Unit = {
    this.trace(MessageFormatter.format(format, arg).getMessage)
  }

  def trace(format: String, arg1: scala.Any, arg2: scala.Any): Unit = {
    this.trace(MessageFormatter.format(format, arg1, arg2).getMessage)
  }

  def trace(format: String, arguments: AnyRef*): Unit = {
    this.trace(MessageFormatter.arrayFormat(format, arguments.toArray).getMessage)
  }

  def trace(msg: String, t: Throwable): Unit = {
    log(msg, Level.TRACE)
    logger.trace(msg, t)
  }

  def isTraceEnabled(marker: Marker): Boolean = logger.isTraceEnabled(marker)

  def trace(marker: Marker, msg: String): Unit = {
    log(msg, Level.TRACE)
    logger.trace(marker, msg)
  }

  def trace(marker: Marker, format: String, arg: scala.Any): Unit = {
    this.trace(marker, MessageFormatter.format(format, arg).getMessage)
  }

  def trace(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = {
    this.trace(marker, MessageFormatter.format(format, arg1, arg2).getMessage)
  }

  def trace(marker: Marker, format: String, argArray: AnyRef*): Unit = {
    this.trace(marker, MessageFormatter.arrayFormat(format, argArray.toArray).getMessage)
  }

  def trace(marker: Marker, msg: String, t: Throwable): Unit = {
    log(msg, Level.TRACE)
    logger.trace(marker, msg, t)
  }

  def isDebugEnabled: Boolean = logger.isDebugEnabled

  def debug(msg: String): Unit = {
    log(msg, Level.DEBUG)
    logger.debug(msg)
  }

  def debug(format: String, arg: scala.Any): Unit = {
    this.debug(MessageFormatter.format(format, arg).getMessage)
  }

  def debug(format: String, arg1: scala.Any, arg2: scala.Any): Unit = {
    this.debug(MessageFormatter.format(format, arg1, arg2).getMessage)
  }

  def debug(format: String, arguments: AnyRef*): Unit = {
    this.debug(MessageFormatter.arrayFormat(format, arguments.toArray).getMessage)
  }

  def debug(msg: String, t: Throwable): Unit = {
    log(msg, Level.DEBUG)
    logger.debug(msg, t)
  }

  def isDebugEnabled(marker: Marker): Boolean = logger.isDebugEnabled(marker)

  def debug(marker: Marker, msg: String): Unit = {
    log(msg, Level.DEBUG)
    logger.debug(marker, msg)
  }

  def debug(marker: Marker, format: String, arg: scala.Any): Unit = {
    this.debug(marker, MessageFormatter.format(format, arg).getMessage)
  }

  def debug(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = {
    this.debug(marker, MessageFormatter.format(format, arg1, arg2).getMessage)
  }

  def debug(marker: Marker, format: String, argArray: AnyRef*): Unit = {
    this.debug(marker, MessageFormatter.arrayFormat(format, argArray.toArray).getMessage)
  }

  def debug(marker: Marker, msg: String, t: Throwable): Unit = {
    log(msg, Level.DEBUG)
    logger.debug(marker, msg, t)
  }

  def isInfoEnabled: Boolean = logger.isInfoEnabled

  def info(msg: String): Unit = {
    log(msg, Level.INFO)
    logger.info(msg)
  }

  def info(format: String, arg: scala.Any): Unit = {
    this.info(MessageFormatter.format(format, arg).getMessage)
  }

  def info(format: String, arg1: scala.Any, arg2: scala.Any): Unit = {
    this.info(MessageFormatter.format(format, arg1, arg2).getMessage)
  }

  def info(format: String, arguments: AnyRef*): Unit = {
    this.info(MessageFormatter.arrayFormat(format, arguments.toArray).getMessage)
  }

  def info(msg: String, t: Throwable): Unit = {
    log(msg, Level.INFO)
    logger.info(msg, t)
  }

  def isInfoEnabled(marker: Marker): Boolean = logger.isInfoEnabled(marker)

  def info(marker: Marker, msg: String): Unit = {
    log(msg, Level.INFO)
    logger.info(marker, msg)
  }

  def info(marker: Marker, format: String, arg: scala.Any): Unit = {
    this.info(marker, MessageFormatter.format(format, arg).getMessage)
  }

  def info(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = {
    this.info(marker, MessageFormatter.format(format, arg1, arg2).getMessage)
  }

  def info(marker: Marker, format: String, argArray: AnyRef*): Unit = {
    this.info(marker, MessageFormatter.arrayFormat(format, argArray.toArray).getMessage)
  }

  def info(marker: Marker, msg: String, t: Throwable): Unit = {
    log(msg, Level.INFO)
    logger.info(marker, msg, t)
  }

  def isWarnEnabled: Boolean = logger.isWarnEnabled

  def warn(msg: String): Unit = {
    log(msg, Level.WARN)
    logger.warn(msg)
  }

  def warn(format: String, arg: scala.Any): Unit = {
    this.warn(MessageFormatter.format(format, arg).getMessage)
  }

  def warn(format: String, arg1: scala.Any, arg2: scala.Any): Unit = {
    this.warn(MessageFormatter.format(format, arg1, arg2).getMessage)
  }

  def warn(format: String, arguments: AnyRef*): Unit = {
    this.warn(MessageFormatter.arrayFormat(format, arguments.toArray).getMessage)
  }

  def warn(msg: String, t: Throwable): Unit = {
    log(msg, Level.WARN)
    logger.warn(msg, t)
  }

  def isWarnEnabled(marker: Marker): Boolean = logger.isWarnEnabled(marker)

  def warn(marker: Marker, msg: String): Unit = {
    log(msg, Level.WARN)
    logger.warn(marker, msg)
  }

  def warn(marker: Marker, format: String, arg: scala.Any): Unit = {
    this.warn(marker, MessageFormatter.format(format, arg).getMessage)
  }

  def warn(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = {
    this.warn(marker, MessageFormatter.format(format, arg1, arg2).getMessage)
  }

  def warn(marker: Marker, format: String, argArray: AnyRef*): Unit = {
    this.warn(marker, MessageFormatter.arrayFormat(format, argArray.toArray).getMessage)
  }

  def warn(marker: Marker, msg: String, t: Throwable): Unit = {
    log(msg, Level.WARN)
    logger.warn(marker, msg, t)
  }

  def isErrorEnabled: Boolean = logger.isErrorEnabled

  def error(msg: String): Unit = {
    log(msg, Level.ERROR)
    logger.error(msg)
  }

  def error(format: String, arg: scala.Any): Unit = {
    this.error(MessageFormatter.format(format, arg).getMessage)
  }

  def error(format: String, arg1: scala.Any, arg2: scala.Any): Unit = {
    this.error(MessageFormatter.format(format, arg1, arg2).getMessage)
  }

  def error(format: String, arguments: AnyRef*): Unit = {
    this.error(MessageFormatter.arrayFormat(format, arguments.toArray).getMessage)
  }

  def error(msg: String, t: Throwable): Unit = {
    log(msg, Level.ERROR)
    logger.error(msg, t)
  }

  def isErrorEnabled(marker: Marker): Boolean = logger.isErrorEnabled(marker)

  def error(marker: Marker, msg: String): Unit = {
    log(msg, Level.ERROR)
    logger.error(marker, msg)
  }

  def error(marker: Marker, format: String, arg: scala.Any): Unit = {
    this.error(marker, MessageFormatter.format(format, arg).getMessage)
  }

  def error(marker: Marker, format: String, arg1: scala.Any, arg2: scala.Any): Unit = {
    this.error(marker, MessageFormatter.format(format, arg1, arg2).getMessage)
  }

  def error(marker: Marker, format: String, argArray: AnyRef*): Unit = {
    this.error(marker, MessageFormatter.arrayFormat(format, argArray.toArray).getMessage)
  }

  def error(marker: Marker, msg: String, t: Throwable): Unit = {
    log(msg, Level.ERROR)
    logger.error(marker, msg, t)
  }
}

object CustomLogger {

  def apply(name: String)(log: (String, Level) => Unit): scalalogging.Logger = scalalogging.Logger(new CustomLogger(LoggerFactory.getLogger(name))(log))

}
