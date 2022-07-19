package com.github.propi.rdfrules.gui.operations.common

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.GreaterThanOrEqualsTo
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}
import com.github.propi.rdfrules.gui.utils.StringConverters._

abstract class CommonShrink(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    val take = new DynamicElement(Constants(new FixedText[Int]("take", "Take", validator = GreaterThanOrEqualsTo[Int](0))))
    val drop = new DynamicElement(Constants(new FixedText[Int]("drop", "Drop", validator = GreaterThanOrEqualsTo[Int](0))))
    val start = new DynamicElement(Constants(new FixedText[Int]("start", "From", validator = GreaterThanOrEqualsTo[Int](0))))
    val end = new DynamicElement(Constants(new FixedText[Int]("end", "Until", validator = GreaterThanOrEqualsTo[Int](0))))

    def activeStrategy(hasTake: Boolean, hasDrop: Boolean, hasSlice: Boolean): Unit = {
      if (hasSlice) {
        start.setElement(0)
        end.setElement(0)
      } else {
        start.setElement(-1)
        end.setElement(-1)
      }
      if (hasTake) take.setElement(0) else take.setElement(-1)
      if (hasDrop) drop.setElement(0) else drop.setElement(-1)
    }

    activeStrategy(true, false, false)

    Constants(
      new Select("name", "Strategy",
        Constants("Take" -> "Take", "Drop" -> "Drop", "Slice" -> "Slice"),
        Some("Take"),
        {
          case "Slice" => activeStrategy(false, false, true)
          case "Drop" => activeStrategy(false, true, false)
          case _ => activeStrategy(true, false, false)
        }
      ),
      take,
      drop,
      start,
      end
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}