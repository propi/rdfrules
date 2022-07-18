package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class FilterQuads(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new DynamicGroup("or", "Filter by (logical OR)", () => Constants(
      new OptionalText[String]("subject", "Subject", description = "Filter for the subject position. If this field is empty then no filter is applied here. The subject must be written in URI format in angle brackets, e.g, <http://dbpedia.org/resource/Rule>, or as a prefixed URI, e.g., dbr:Rule. The content is evaluated as a regular expression.", validator = RegExp("<.*>|.*:.*", true)),
      new OptionalText[String]("predicate", "Predicate", description = "Filter for the predicate position. If this field is empty then no filter is applied here. The predicate must be written in URI format in angle brackets, e.g, <https://www.w3.org/2000/01/rdf-schema#label>, or as a prefixed URI, e.g., rdfs:label. The content is evaluated as a regular expression.", validator = RegExp("<.*>|.*:.*", true)),
      new OptionalText[String]("object", "Object", description = ""),
      new OptionalText[String]("graph", "Graph", description = "", validator = RegExp("<.*>|.*:.*", true)),
      new Checkbox("inverse", "Negation", description = "")
    ), "Defined quad filters. It filters such quads which satisfy defined conditions.")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}