package com.github.propi.rdfrules.gui.operations.actions

import com.github.propi.rdfrules.gui.operations.common.CommonActionCache
import com.github.propi.rdfrules.gui.{Operation, OperationInfo}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class CacheIndex(fromOperation: Operation) extends CommonActionCache(fromOperation, OperationInfo.CacheIndexAction)