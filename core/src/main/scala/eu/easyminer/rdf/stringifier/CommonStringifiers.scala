package eu.easyminer.rdf.stringifier

import eu.easyminer.rdf.data.Triple

/**
  * Created by Vaclav Zeman on 15. 1. 2018.
  */
object CommonStringifiers {

  implicit val tripleStringifier: Stringifier[Triple] = (v: Triple) => v.subject + "  " + v.predicate + "  " + v.`object`

}
