package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.utils.HowLong


/**
  * Created by Vaclav Zeman on 14. 5. 2019.
  */
object Benchmark {

  /*def apply[I, O1, O2](input: I, task: Task[I, O1, O2])(implicit toM1: O2 => Seq[Metric], toM2: HowLong.Stats => Seq[Metric]): Seq[Metric] = {
    val (stats, result) = task.execute(input)
    toM1(result) ++ toM2(stats)
  }*/

}
