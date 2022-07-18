package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators._
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.language.implicitConversions

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Discretize(fromOperation: Operation, val info: OperationInfo) extends Operation {

  val properties: Constants[Property] = {
    val bins = new DynamicElement(Constants(new FixedText[Int]("bins", "Number of bins", description = "Number of intervals to be created.", validator = GreaterThan[Int](0))))
    val support = new DynamicElement(Constants(new FixedText[Double]("support", "Min support", description = "The minimal relative support which must reach each interval. The valid range is between 0 and 1.", validator = GreaterThanOrEqualsTo(0.0).map[String] & LowerThanOrEqualsTo(1.0).map[String])))

    def activeStrategy(hasBins: Boolean, hasSupport: Boolean): Unit = {
      if (hasBins) bins.setElement(0) else bins.setElement(-1)
      if (hasSupport) support.setElement(0) else support.setElement(-1)
    }

    Constants(
      new OptionalText[String]("subject", "Subject", description = "Discretize all numeric literals which are related to this specifed subject. If this field is empty then no filter is applied here. The subject must be written in URI format in angle brackets, e.g, <http://dbpedia.org/resource/Rule>, or as a prefixed URI, e.g., dbr:Rule.", validator = RegExp("<.*>|\\w+:.*", true)),
      new OptionalText[String]("predicate", "Predicate", description = "Discretize all numeric literals which are related to this specifed predicate. If this field is empty then no filter is applied here. The predicate must be written in URI format in angle brackets, e.g, <https://www.w3.org/2000/01/rdf-schema#label>, or as a prefixed URI, e.g., rdfs:label.", validator = RegExp("<.*>|\\w+:.*", true)),
      new OptionalText[String]("object", "Object", description = "Discretize all numeric literals which are matching this object. If this field is empty then no filter is applied here. The object must be a numeric comparison, e.g, '> 10' or '(10;80]'.", validator = RegExp("[><]=? \\d+(\\.\\d+)?|[\\[\\(]\\d+(\\.\\d+)?;\\d+(\\.\\d+)?[\\]\\)]", true)),
      new OptionalText[String]("graph", "Graph", description = "Discretize all numeric literals which are related to this specifed graph. If this field is empty then no filter is applied here. The graph must be written in URI format in angle brackets, e.g, <http://dbpedia.org>.", validator = RegExp("<.*>|\\w+:.*", true)),
      new Checkbox("inverse", "Negation", description = "If this field is checked then all defined filters (above) are negated (logical NOT is applied before all filters)."),
      new Group("task", "Strategy", Constants(
        new Select("name", "Name",
          Constants("EquidistanceDiscretizationTask" -> "Equidistance", "EquifrequencyDiscretizationTask" -> "Equifrequency", "EquisizeDiscretizationTask" -> "Equisize"),
          onSelect = {
            case "EquisizeDiscretizationTask" => activeStrategy(false, true)
            case "EquidistanceDiscretizationTask" | "EquifrequencyDiscretizationTask" => activeStrategy(true, false)
            case _ => activeStrategy(false, false)
          }
        ),
        bins,
        support
      ))
    )
  }

  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}