package com.github.propi.rdfrules.data

import org.apache.jena.riot.Lang

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 27. 6. 2017.
  */
sealed trait RdfSource

object RdfSource {

  object Tsv extends RdfSource

  case class JenaLang(lang: Lang) extends RdfSource

}