package com.github.propi.rdfrules.http.util

import com.typesafe.config.{Config, ConfigFactory}
import configs.syntax._
import configs.{ConfigReader, Result}

/**
  * Created by Vaclav Zeman on 13. 8. 2017.
  */
object Conf {

  private val conf = ConfigFactory.load()

  def apply[A](path: String)(implicit a: ConfigReader[A]): Result[A] = conf.get(path)

  def apply[A](config: Config, path: String)(implicit a: ConfigReader[A]): Result[A] = config.get(path)

}