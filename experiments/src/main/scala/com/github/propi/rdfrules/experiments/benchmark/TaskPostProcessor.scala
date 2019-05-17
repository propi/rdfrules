package com.github.propi.rdfrules.experiments.benchmark

/**
  * Created by Vaclav Zeman on 17. 5. 2019.
  */
trait TaskPostProcessor[I, O] {

  protected def postProcess(result: I): O

}
