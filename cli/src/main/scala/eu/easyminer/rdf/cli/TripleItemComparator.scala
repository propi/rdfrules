package eu.easyminer.rdf.cli

import eu.easyminer.rdf.data.TripleItem
import eu.easyminer.rdf.utils.BasicExtractors.{AnyToBoolean, AnyToDouble}

/**
  * Created by Vaclav Zeman on 7. 10. 2017.
  */
object TripleItemComparator {

  def compare(tripleItem: TripleItem, string: String): Boolean = tripleItem match {
    case TripleItem.LongUri(uri) => uri == string
    case TripleItem.PrefixedUri(prefix, nameSpace, localName) => string == s"$prefix:$localName" || string == (nameSpace + localName)
    case TripleItem.BlankNode(id) => string == s"_:$id"
    case TripleItem.Text(v) => string == v
    case TripleItem.BooleanValue(v) => AnyToBoolean.unapply(string).contains(v)
    case TripleItem.Number(AnyToDouble(v)) => AnyToDouble.unapply(string).contains(v)
    case _ => false
  }

}
