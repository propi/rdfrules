package com.github.propi.rdfrules.printer

import com.github.propi.rdfrules.data.Triple
import com.github.propi.rdfrules.stringifier.Stringifier

/**
  * Created by Vaclav Zeman on 15. 1. 2018.
  */
object CommonPrinters {

  implicit def tripleDataPrinter(implicit str: Stringifier[Triple], strPrinter: Printer[String]): Printer[Triple] = (v: Triple) => strPrinter.println(str.toStringValue(v))

}