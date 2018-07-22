package com.github.propi.rdfrules.http.util

import com.typesafe.config.ConfigFactory
import configs.{Configs, Result}
import configs.syntax._

/**
  * Created by Vaclav Zeman on 13. 8. 2017.
  */
object Conf {

  private val conf = ConfigFactory.load()

  def apply[A](path: String)(implicit a: Configs[A]): Result[A] = conf.get(path)

}