package com.github.propi.rdfrules.http.util

/**
  * Created by Vaclav Zeman on 13. 8. 2017.
  */
trait DefaultServerConf {

  val configServerPrefix: String

  lazy val host: String = Conf[String](configServerPrefix + ".host").value
  lazy val port: Int = Conf[Int](configServerPrefix + ".port").value
  lazy val rootPath: String = Conf[String](configServerPrefix + ".root-path").value
  lazy val stoppingToken: String = Conf[String](configServerPrefix + ".stopping-token").value

}