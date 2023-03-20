package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.{ArrayElement, FixedText}
import com.github.propi.rdfrules.gui.utils.CommonValidators.RegExp
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Index(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    ArrayElement("train", "Training set (URIs)")(_ => new FixedText[String]("uri", "URI", validator = RegExp("<.*>|.*:.*", true))),
    ArrayElement("test", "Test set (URIs)")(_ => new FixedText[String]("uri", "URI", validator = RegExp("<.*>|.*:.*", true)))
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}