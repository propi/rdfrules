package com.github.propi.rdfrules.gui.properties

import com.thoughtworks.binding.Binding.Constants

/**
  * Created by Vaclav Zeman on 17. 9. 2018.
  */
object Pattern {

  private def atomItemPatternProperties = {
    val value = new DynamicElement(Constants(
      new FixedText[String]("value", "Value"),
      new DynamicGroup("value", "Values", () => {
        val innerValue = new DynamicElement(Constants(new FixedText[String]("value", "Value")))
        Constants(
          new Select("name", "Name", Constants("AnyConstant" -> "AnyConstant", "AnyVariable" -> "AnyVariable", "Constant" -> "Constant", "Variable" -> "Variable"), onSelect = {
            case "Constant" | "Variable" => innerValue.setElement(0)
            case _ => innerValue.setElement(-1)
          }),
          innerValue
        )
      })
    ))
    Constants(
      new Select("name", "Name", Constants("Any" -> "Any", "AnyConstant" -> "AnyConstant", "AnyVariable" -> "AnyVariable", "Constant" -> "Constant", "Variable" -> "Variable", "OneOf" -> "OneOf", "NoneOf" -> "NoneOf"), Some("Any"), {
        case "Constant" | "Variable" => value.setElement(0)
        case "OneOf" | "NoneOf" => value.setElement(1)
        case _ => value.setElement(-1)
      }),
      value
    )
  }

  private def atomPatternProperties = Constants(
    new Group("subject", "Subject", atomItemPatternProperties),
    new Group("predicate", "Predicate", atomItemPatternProperties),
    new Group("object", "Object", atomItemPatternProperties),
    new Group("graph", "Graph", atomItemPatternProperties)
  )

  def apply() = Constants(
    new Group("head", "Head", atomPatternProperties),
    new DynamicGroup("body", "Body", () => atomPatternProperties),
    new Checkbox("exact", "Exact")
  )

}
