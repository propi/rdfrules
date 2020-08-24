package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties._
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class Prune(fromOperation: Operation, val info: OperationInfo) extends Operation {
  val properties: Constants[Property] = Constants(
    new Checkbox("onlyFunctionalProperties", "Only functional properties", true, "Generate only functional properties. That means only one object can be predicted for pair (subject, predicate)."),
    new Checkbox("onlyExistingTriples", "Only existing triples", true, "If checked, the common CBA strategy will be used. That means we take only such predicted triples, which are contained in the input dataset. This strategy takes maximally as much memory as the number of triples in the input dataset. If false, we take all predicted triples (including triples which are not contained in the input dataset and are newly generated). For deduplication a HashSet is used and therefore the memory may increase unexpectedly because we need to save all unique generated triples into memory..")
  )
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))
}