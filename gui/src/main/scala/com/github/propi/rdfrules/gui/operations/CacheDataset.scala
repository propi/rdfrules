package com.github.propi.rdfrules.gui.operations

import com.github.propi.rdfrules.gui.operations.common.CommonCache
import com.github.propi.rdfrules.gui.{Operation, OperationInfo}

/**
  * Created by Vaclav Zeman on 21. 7. 2018.
  */
class CacheDataset(fromOperation: Operation, info: OperationInfo, id: Option[String]) extends CommonCache(fromOperation, info, id)