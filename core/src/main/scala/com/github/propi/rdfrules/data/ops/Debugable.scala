package com.github.propi.rdfrules.data.ops

import com.github.propi.rdfrules.utils.Debugger

/**
  * Created by Vaclav Zeman on 27. 2. 2018.
  */
trait Debugable[T, Coll] extends Transformable[T, Coll] {

  self: Coll =>

  protected val dataLoadingText: String

  /**
    * Cache the entity into the memory and return cached entity (IndexedSeq abstraction is used)
    * Strict transformation
    *
    * @return in memory cached entity
    */
  def withDebugger(implicit debugger: Debugger): Coll = transform(coll.withDebugger(dataLoadingText))

}