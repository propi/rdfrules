package com.github.propi.rdfrules.data.ops

import com.github.propi.rdfrules.utils.{Debugger, ForEach}

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
  def withDebugger(implicit debugger: Debugger): Coll = transform(new ForEach[T] {
    def foreach(f: T => Unit): Unit = {
      debugger.debug(dataLoadingText) { ad =>
        for (x <- coll.takeWhile(_ => !debugger.isInterrupted)) {
          f(x)
          ad.done()
        }
        if (debugger.isInterrupted) {
          debugger.logger.warn(s"The loading task has been interrupted. The loaded data may not be complete.")
        }
      }
    }

    override def knownSize: Int = coll.knownSize
  })

}