package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.operations.common.EmptyOperation
import com.github.propi.rdfrules.gui.{Operation, OperationInfo}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class GraphAwareRules(fromOperation: Operation, info: OperationInfo) extends EmptyOperation(fromOperation, info)