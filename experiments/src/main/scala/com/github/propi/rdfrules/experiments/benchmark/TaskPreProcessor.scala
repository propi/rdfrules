package com.github.propi.rdfrules.experiments.benchmark

/**
  * Created by Vaclav Zeman on 17. 5. 2019.
  */
trait TaskPreProcessor[I, O] {

  protected def preProcess(input: I): O

}
