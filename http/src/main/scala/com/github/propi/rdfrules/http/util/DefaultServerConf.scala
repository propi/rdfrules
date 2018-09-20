package com.github.propi.rdfrules.http.util

import java.io.File

/**
  * Created by Vaclav Zeman on 13. 8. 2017.
  */
trait DefaultServerConf {

  val configServerPrefix: String

  lazy val host: String = Conf[String](configServerPrefix + ".host").value
  lazy val port: Int = Conf[Int](configServerPrefix + ".port").value
  lazy val rootPath: String = Conf[String](configServerPrefix + ".root-path").value
  lazy val stoppingToken: String = Conf[String](configServerPrefix + ".stopping-token").value
  lazy val webappDir: Option[String] = Conf[String](configServerPrefix + ".webapp-dir").map(Option.apply).value.map(_.trim).filter(x => x.nonEmpty && new File(x).isDirectory)

}