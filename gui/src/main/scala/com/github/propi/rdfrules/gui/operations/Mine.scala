package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.{GreaterThanOrEqualsTo, LowerThanOrEqualsTo, NonEmpty, RegExp}
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding
import com.thoughtworks.binding.Binding.{Constants, Var}
import org.lrng.binding.html
import org.scalajs.dom.html.Div

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Mine(fromOperation: Operation, val info: OperationInfo) extends Operation {

  private object RuleConsumers extends Property {
    val name: String = "ruleConsumers"
    val title: String = "Rule consumers"
    val descriptionVar: Var[String] = Var(context(title).description)

    private var hasTopK: Boolean = false
    private var hasOnDisk: Boolean = false
    private var selectedFormat: String = "ndjson"

    private val properties = context.use(title){ implicit context =>
      val k = new DynamicElement(Constants(new FixedText[Double]("k", "k-value", validator = GreaterThanOrEqualsTo[Int](1))), hidden = true)
      val allowOverflow = new DynamicElement(Constants(new Checkbox("allowOverflow", "Allow overflow")), hidden = true)
      val file = new DynamicElement(Constants(new FixedText[String]("file", "Export path", validator = NonEmpty)), hidden = true)
      val format = new DynamicElement(Constants(new Select("format", "Export rules format", Constants("txt" -> "Text (unparsable)", "ndjson" -> "streaming NDJSON (as model - parsable)"), Some(selectedFormat), selectedFormat = _)), hidden = true)
      Constants(
        new Checkbox("topk", "Top-k", onChecked = { isChecked =>
          hasTopK = isChecked
          if (isChecked) {
            k.setElement(0)
            allowOverflow.setElement(0)
          } else {
            k.setElement(-1)
            allowOverflow.setElement(-1)
          }
        }),
        k,
        allowOverflow,
        new Checkbox("onDisk", "On disk", onChecked = { isChecked =>
          hasOnDisk = isChecked
          if (isChecked) {
            file.setElement(0)
            format.setElement(0)
          } else {
            file.setElement(-1)
            format.setElement(-1)
          }
        }),
        file,
        format
      )
    }

    @html
    def valueView: Binding[Div] = <div class="properties sub">
      <table>
        {for (property <- properties) yield {
        property.view.bind
      }}
      </table>
    </div>

    def validate(): Option[String] = properties.value.iterator.map(_.validate()).find(_.nonEmpty).flatten.map(x => s"There is an error within '$title' properties: $x")

    def setValue(data: js.Dynamic): Unit = {
      for (x <- data.asInstanceOf[js.Array[js.Dynamic]]) {
        if (x.name.asInstanceOf[String] == "topK") {
          properties.value.head.setValue(js.Any.fromBoolean(true).asInstanceOf[js.Dynamic])
          properties.value(1).setValue(x.k)
          properties.value(2).setValue(x.allowOverflow)
        }
        if (x.name.asInstanceOf[String] == "onDisk") {
          properties.value(3).setValue(js.Any.fromBoolean(true).asInstanceOf[js.Dynamic])
          properties.value(4).setValue(x.file)
          properties.value(5).setValue(x.format)
        }
      }
    }

    def toJson: js.Any = {
      val consumers = js.Array[js.Any]()
      if (hasTopK) {
        consumers.push(js.Dictionary("name" -> "topK", properties.value(1).name -> properties.value(1).toJson, properties.value(2).name -> properties.value(2).toJson))
      } else if (!hasOnDisk || selectedFormat != "ndjson") {
        consumers.push(js.Dictionary("name" -> "inMemory"))
      }
      if (hasOnDisk) {
        consumers.push(js.Dictionary("name" -> "onDisk", properties.value(4).name -> properties.value(4).name, properties.value(5).name -> properties.value(5).name))
      }
      consumers
    }
  }

  val properties: Constants[Property] = {
    val thresholds = DynamicGroup("thresholds", "Thresholds") { implicit context =>
      val value = new DynamicElement(Constants(
        context.use("MinHeadCoverage")(implicit context => new FixedText[Double]("value", "Value", default = "0.1", validator = GreaterThanOrEqualsTo(0.001).map[String] & LowerThanOrEqualsTo(1.0).map[String])),
        context.use("MinHeadSize or MinSupport or Timeout")(implicit context => new FixedText[Double]("value", "Value", validator = GreaterThanOrEqualsTo[Int](1))),
        context.use("MaxRuleLength")(implicit context => new FixedText[Double]("value", "Value", validator = GreaterThanOrEqualsTo[Int](2))),
        context.use("MinAtomSize")(implicit context => new FixedText[Double]("value", "Value", default = "-1"))
      ))
      Constants(
        new Select(
          "name",
          "Name",
          Constants("MinHeadSize" -> "Min head size", "MinAtomSize" -> "Min atom size", "MinHeadCoverage" -> "Min head coverage", "MinSupport" -> "Min support", "MaxRuleLength" -> "Max rule length", "Timeout" -> "Timeout"),
          onSelect = {
            case "MinHeadCoverage" => value.setElement(0)
            case "MinHeadSize" | "MinSupport" | "Timeout" => value.setElement(1)
            case "MaxRuleLength" => value.setElement(2)
            case "MinAtomSize" => value.setElement(3)
            case _ => value.setElement(-1)
          }
        ),
        value
      )
    }
    val constraints = DynamicGroup("constraints", "Constraints") { implicit context =>
      val value = new DynamicElement(Constants(
        context.use("OnlyPredicates or WithoutPredicates")(implicit context => ArrayElement("values", "Values")(implicit context => new OptionalText[String]("value", "Value", validator = RegExp("<.*>|\\w+:.*"))))
      ))
      Constants(
        new Select("name", "Name", Constants("WithoutConstants" -> "Without constants", "OnlyObjectConstants" -> "With constants at the object position", "OnlySubjectConstants" -> "With constants at the subject position", "OnlyLeastFunctionalConstants" -> "With constants at the least functional position", "WithoutDuplicitPredicates" -> "Without duplicit predicates", "OnlyPredicates" -> "Only predicates", "WithoutPredicates" -> "Without predicates"), onSelect = {
          case "OnlyPredicates" | "WithoutPredicates" => value.setElement(0)
          case _ => value.setElement(-1)
        }),
        value
      )
    }
    thresholds.setValue(js.Array(
      js.Dictionary("name" -> "MinHeadSize", "value" -> 100),
      js.Dictionary("name" -> "MinHeadCoverage", "value" -> 0.01),
      js.Dictionary("name" -> "MaxRuleLength", "value" -> 3),
      js.Dictionary("name" -> "Timeout", "value" -> 5)
    ).asInstanceOf[js.Dynamic])
    constraints.setValue(js.Array(js.Dictionary("name" -> "WithoutConstants")).asInstanceOf[js.Dynamic])
    Constants(
      thresholds,
      RuleConsumers,
      Pattern("patterns", "Patterns"),
      constraints,
      new FixedText[Int]("parallelism", "Parallelism", "0", GreaterThanOrEqualsTo[Int](0))
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}