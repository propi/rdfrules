package eu.easyminer.rdf.printer

import eu.easyminer.rdf.data.Triple
import eu.easyminer.rdf.stringifier.Stringifier

/**
  * Created by Vaclav Zeman on 15. 1. 2018.
  */
object CommonPrinters {

  implicit def tripleDataPrinter(implicit str: Stringifier[Triple], strPrinter: Printer[String]): Printer[Triple] = (v: Triple) => strPrinter.println(str.toStringValue(v))

}