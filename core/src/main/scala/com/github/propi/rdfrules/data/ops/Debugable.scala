package com.github.propi.rdfrules.data.ops

import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 27. 2. 2018.
  */
trait Debugable[T, Coll] extends Transformable[T, Coll] {

  self: Coll =>

  protected def dataLoadingText: String

  def withDebugger(msg: String = dataLoadingText, forced: Boolean = false)(implicit debugger: Debugger): Coll = transform(coll.withDebugger(msg, forced))

}