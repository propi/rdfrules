package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.{GreaterThanOrEqualsTo, LowerThanOrEqualsTo, NonEmpty, RegExp}
import com.github.propi.rdfrules.gui.utils.StringConverters._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

import scala.scalajs.js

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Mine(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    val thresholds = new DynamicGroup("thresholds", "Thresholds", () => {
      val value = new DynamicElement(Constants(
        new FixedText[Double]("value", "Value", default = "0.1", description = "The minimal value is 0.001 and maximal value is 1.", validator = GreaterThanOrEqualsTo(0.001).map[String] & LowerThanOrEqualsTo(1.0).map[String]),
        new FixedText[Double]("value", "Value", description = "The minimal value is 1.", validator = GreaterThanOrEqualsTo[Int](1)),
        new FixedText[Double]("value", "Value", description = "The minimal value is 2.", validator = GreaterThanOrEqualsTo[Int](2)),
        new FixedText[Double]("value", "Value", description = "If negative value, the minimal atom size is same as the current minimal support threshold.", default = "-1")
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
    }, description = "Mining thresholds. For one mining task you can specify several thresholds. All mined rules must reach defined thresholds. This greatly affects the mining time. Default thresholds are MinHeadSize=100 (if MinHeadSize is not defined), MinSupport=1 (if MinSupport and MinHeadCovarage are not defined), MaxRuleLength=3 (if MaxRuleLength is not defined).")
    val constraints = new DynamicGroup("constraints", "Constraints", () => {
      val value = new DynamicElement(Constants(
        new ArrayElement("values", "Values", () => new OptionalText[String]("value", "Value", validator = RegExp("<.*>|\\w+:.*")), description = "List of predicates. You can use prefixed URI or full URI in angle brackets.")
      ))
      Constants(
        new Select("name", "Name", Constants("WithoutConstants" -> "Without constants", "OnlyObjectConstants" -> "With constants at the object position", "OnlySubjectConstants" -> "With constants at the subject position", "OnlyLeastFunctionalConstants" -> "With constants at the least functional position", "WithoutDuplicitPredicates" -> "Without duplicit predicates", "OnlyPredicates" -> "Only predicates", "WithoutPredicates" -> "Without predicates"), onSelect = {
          case "OnlyPredicates" | "WithoutPredicates" => value.setElement(0)
          case _ => value.setElement(-1)
        }),
        value
      )
    }, description = "Within constraints you can specify whether to mine rules without constants or you can include only chosen predicates into the mining phase.")
    thresholds.setValue(js.Array(
      js.Dictionary("name" -> "MinHeadSize", "value" -> 100),
      js.Dictionary("name" -> "MinHeadCoverage", "value" -> 0.01),
      js.Dictionary("name" -> "MaxRuleLength", "value" -> 3),
      js.Dictionary("name" -> "Timeout", "value" -> 5)
    ).asInstanceOf[js.Dynamic])
    constraints.setValue(js.Array(js.Dictionary("name" -> "WithoutConstants")).asInstanceOf[js.Dynamic])
    val pfile = new DynamicElement(Constants(new FixedText[String]("pfile", "Export path", description = "A relative path to a file where the rules will be continuously saved in a pretty printed format.", validator = NonEmpty)), hidden = true)
    val pformat = new DynamicElement(Constants(new Select("format", "Export rules format", Constants("txt" -> "Text (unparsable)", "ndjson" -> "streaming NDJSON (as model - parsable)"), Some("ndjson"))), hidden = true)
    val file = new DynamicElement(Constants(new FixedText[String]("file", "Cache path", description = "A relative path to a file where the rules will be continuously saved in the cache format.", validator = NonEmpty)), hidden = true)
    val k = new DynamicElement(Constants(new FixedText[Double]("k", "Top-k", description = "A k value for the top-k approach. The minimal value is 1.", validator = GreaterThanOrEqualsTo[Int](1))), hidden = true)
    val allowOverflow = new DynamicElement(Constants(new Checkbox("allowOverflow", "Allow overflow", description = "If there are multiple rules with the lowest head coverage in the priority queue, then all of them may not be saved into the queue since the k value can not be exceeded. For this case the ordering of such rules is not deterministic and same task can return different results due to the long tail of rules with a same head coverage. If you check this, the overflowed long tail will be also returned but you can get much more rules on the output than the k value.")), hidden = true)
    val ruleConsumer = new Group("ruleConsumer", "Rule consumer", Constants(
      new Select(
        "name",
        "Type",
        Constants("inMemory" -> "In memory", "onDisk" -> "On disk", "topK" -> "Top-k"),
        Some("inMemory"),
        {
          case "onDisk" =>
            file.setElement(0)
            k.setElement(-1)
            allowOverflow.setElement(-1)
          case "topK" =>
            file.setElement(-1)
            k.setElement(0)
            allowOverflow.setElement(0)
          case _ =>
            file.setElement(-1)
            k.setElement(-1)
            allowOverflow.setElement(-1)
        },
        "In memory: all mined rules are saved into a memory cache. On disk: all mined rules are saved on disk (in the cache format). Top-k: all mined rules are saved into the top-k priority queue in memory."
      ),
      file,
      k,
      allowOverflow,
      new Checkbox(
        "pfileOn",
        "Export rules",
        description = "If checked mined rules are continuously saved into a file (in a pretty printed format) during mining. If the task fails you do not lose yet mined rules.",
        onChecked = isChecked => {
          if (isChecked) {
            pfile.setElement(0)
            pformat.setElement(0)
          } else {
            pfile.setElement(-1)
            pformat.setElement(-1)
          }
        }),
      pfile,
      pformat,
    ), "The consumer which processes all mined closed rules satisfying all thresholds, patterns and constraints.")
    Constants(
      thresholds,
      ruleConsumer,
      new DynamicGroup("patterns", "Patterns", () => Pattern(), description = "In this property, you can define several rule patterns. During the mining phase, each rule must match at least one of the defined patterns."),
      constraints,
      new FixedText[Int]("parallelism", "Parallelism", "0", "If the value is lower than or equal to 0 and greater than 'all available cores' then the parallelism level is set to 'all available cores'.", GreaterThanOrEqualsTo[Int](0))
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}