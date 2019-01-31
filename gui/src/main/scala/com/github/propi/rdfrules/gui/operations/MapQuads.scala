package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class MapQuads(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.MapQuads
  val properties: Constants[Property] = Constants(
    new Group("search", "Search", Constants(
      new OptionalText[String]("subject", "Subject", description = "Filter for the subject position. If this field is empty then no filter is applied here. The subject must be written in URI format in angle brackets, e.g, <http://dbpedia.org/resource/Rule>, or as a prefixed URI, e.g., dbr:Rule. The content is evaluated as a regular expression.", validator = RegExp("<.*>|.*:.*", true)),
      new OptionalText[String]("predicate", "Predicate", description = "Filter for the predicate position. If this field is empty then no filter is applied here. The predicate must be written in URI format in angle brackets, e.g, <https://www.w3.org/2000/01/rdf-schema#label>, or as a prefixed URI, e.g., rdfs:label. The content is evaluated as a regular expression.", validator = RegExp("<.*>|.*:.*", true)),
      new OptionalText[String]("object", "Object", description = "Filter for the object position. If this field is empty then no filter is applied here. The content is evaluated as a regular expression. You can filter resources (with regexp) and literals (with regexp and conditions). Literals can be text, number, boolean or interval. For TEXT, the content must be in double quotation marks. For NUMBER, you can use exact matching or conditions, e.g., '> 10' or intervals [10;80). For BOOLEAN, there are valid only two values true|false. For INTERVAL, you can use only exact matching like this: i[10;50); it must start with 'i' character."),
      new OptionalText[String]("graph", "Graph", description = "Filter for the graph position. If this field is empty then no filter is applied here. The graph must be written in URI format in angle brackets, e.g, <http://dbpedia.org>. The content is evaluated as a regular expression.", validator = RegExp("<.*>|.*:.*", true)),
      new Checkbox("inverse", "Negation", description = "If this field is checked then all defined filters (above) are negated (logical NOT is applied before all filters).")
    ), description = "Search quads by this filter and then replace found quads by defined replacements. Some filters can capture parts of quads and their contents."),
    new Group("replacement", "Replacement", Constants(
      new OptionalText[String]("subject", "Subject", description = "Replacement can be only the full URI or blank node (prefixed URI is not allowed). If this field is empty then no replace is applied here. You can refer to captured parts and groups of found quad, e.g, $0 = the full match, $1 = captured group 1 in regexp, $p1 = captured group in regexp in the predicate position.", validator = RegExp("<.*>|_:.*", true)),
      new OptionalText[String]("predicate", "Predicate", description = "Replacement can be only the full URI or blank node (prefixed URI is not allowed). If this field is empty then no replace is applied here. You can refer to captured parts and groups of found quad, e.g, $0 = the full match, $1 = captured group 1 in regexp, $s1 = captured group in regexp in the subject position.", validator = RegExp("<.*>|_:.*", true)),
      new OptionalText[String]("object", "Object", description = "For RESOURCE, the replacement can be only the full URI or blank node (prefixed URI is not allowed). For TEXT, the replacement must start and end with double quotes. For NUMBER, the replacement must be a number or some arithmetic evaluation with captured value, e.g., $o0 + 5 (it adds 5 to original numeric value). For BOOLEAN, there are only two valid values true|false. For INTERVAL, the replacement has the interval form, e.g, (10;80] (both borders of the found interval are captured, we can refer to them in replacement: [$o1;$o2]). If this field is empty then no replace is applied here. You can refer to captured parts and groups of found quad, e.g, $0 = the full match, $1 = captured group 1 in regexp, $s1 = captured group in regexp in the subject position."),
      new OptionalText[String]("graph", "Graph", description = "Replacement can be only the full URI or blank node (prefixed URI is not allowed). If this field is empty then no replace is applied here. You can refer to captured parts and groups of found quad, e.g, $0 = the full match, $1 = captured group 1 in regexp, $s1 = captured group in regexp in the subject position.", validator = RegExp("<.*>|_:.*", true))
    ), description = "Replace found quads and their parts with replacements. Here, you can also refer to captured parts in regular expressions.")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}