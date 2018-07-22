package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.properties.ChooseFileFromWorkspace
import com.github.propi.rdfrules.gui.{Operation, OperationInfo, Property}
import com.thoughtworks.binding.Binding.{Constants, Var}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class LoadGraph(fromOperation: Operation) extends Operation {
  val info: OperationInfo = OperationInfo.loadGraph
  val `type`: Var[Operation.Type] = Var(Operation.Type.Transformation)
  val properties: Constants[Property] = Constants(ChooseFileFromWorkspace(Constants("soubor1", "soubor2", "soubor3")))
  val previousOperation: Var[Option[Operation]] = Var(Some(fromOperation))

  protected def followingOperations: Constants[OperationInfo] = Constants(OperationInfo.loadGraph)

  protected def buildFollowingOperation(operationInfo: OperationInfo): Operation = new LoadGraph(this)
}
