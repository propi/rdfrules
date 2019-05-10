package com.github.propi.rdfrules.utils.workers

/**
  * Created by Vaclav Zeman on 9. 5. 2019.
  */
trait WorkerBuilder[I, O] {

  def build(workerId: Int, resultConsumer: ResultConsumer[O]): WorkerActor[I, O]

}
