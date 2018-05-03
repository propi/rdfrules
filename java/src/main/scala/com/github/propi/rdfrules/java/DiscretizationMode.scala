package com.github.propi.rdfrules.java

import com.github.propi.rdfrules.data.ops

/**
  * Created by Vaclav Zeman on 3. 5. 2018.
  */
object DiscretizationMode {

  def onDisc: ops.Discretizable.DiscretizationMode = ops.Discretizable.DiscretizationMode.OnDisc

  def inMemory: ops.Discretizable.DiscretizationMode = ops.Discretizable.DiscretizationMode.InMemory

}
