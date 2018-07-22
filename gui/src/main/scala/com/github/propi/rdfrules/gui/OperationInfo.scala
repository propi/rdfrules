package com.github.propi.rdfrules.gui

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
case class OperationInfo(name: String, title: String, types: Set[Operation.Type])

object OperationInfo {

  val loadGraph = OperationInfo("load-graph", "Load graph", Set(Operation.Type.Transformation))

}