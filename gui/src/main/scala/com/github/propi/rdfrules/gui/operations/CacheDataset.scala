package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.utils.CommonValidators.NonEmpty
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

import java.util.UUID

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class CacheDataset(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = {
    val path = new DynamicElement(Constants(
      new FixedText[String]("path", "Path or Cache ID", description = "A relative path to a file related to the workspace where the serialized dataset should be saved.", validator = NonEmpty),
      new FixedText[String]("path", "Path or Cache ID", description = "The cache identifier in the memory.", validator = NonEmpty, default = UUID.randomUUID().toString)
    ))
    path.setElement(1)
    Constants(
      new Checkbox("inMemory", "In-memory", true, "Choose whether to save all previous transformations into memory or disk.", {
        case true => path.setElement(1)
        case false => path.setElement(0)
      }),
      path,
      new Checkbox("revalidate", "Revalidate", description = "Check this if you want to re-create the cache from the previous transformations.")
    )
  }
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}