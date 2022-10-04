package com.github.propi.rdfrules.experiments.benchmark

/**
  * Created by Vaclav Zeman on 14. 5. 2019.
  */
object Benchmark {

  implicit class PimpedInt(x: Int) {
    def times: Times = new Times(x)
  }

  class Times(x: Int) {
    def executeTask[I, O](task: Task[I, _, _, O]): (String, LazyList[Task[I, _, _, O]]) = task.name -> LazyList.fill(x)(task)
  }

  object Once {
    def executeTask[I, O](task: Task[I, _, _, O]): (String, Task[I, _, _, O]) = task.name -> task
  }

  implicit class PimpedTask[I, O](taskStream: (String, Task[I, _, _, O])) {
    def withInput(input: I)
                 (implicit m2: O => Seq[Metric]): (String, O, Seq[Metric]) = {
      val (gm, om) = taskStream._2.execute(input)
      (taskStream._1, om, gm ++ m2(om))
    }
  }

  implicit class PimpedTaskStream[I, O](taskStream: (String, LazyList[Task[I, _, _, O]])) {
    def withInput(input: I)
                 (implicit m2: O => Seq[Metric]): (String, LazyList[Seq[Metric]]) = {
      taskStream._1 -> taskStream._2
        .map(_.execute(input))
        .map(x => x._1 ++ m2(x._2))
    }
  }

  implicit class PimpedMetricStream(metricStream: (String, LazyList[Seq[Metric]])) {
    def andAggregateResultWith(metricsAggregator: MetricsAggregator): (String, Seq[Metric]) = {
      metricStream._1 -> metricsAggregator.aggregateMetrics(metricStream._2)
    }
  }

  implicit class PimpedMetricResult(metricResult: (String, Seq[Metric])) {
    def andFinallyProcessResultWith[T](metricResultProcessor: MetricResultProcessor[T]): T = metricResultProcessor.processMetrics(metricResult)

    def compareWith(metricResult2: (String, Seq[Metric])): ((String, Seq[Metric]), (String, Seq[Metric])) = {
      val m1 = metricResult._2.groupBy(_.name).view.mapValues(_.head)
      val m2 = metricResult2._2.groupBy(_.name).view.mapValues(_.head)
      val r1 = metricResult._2.flatMap(x => m2.get(x.name).map(y => Metric.Comparison(x, y)))
      val r2 = metricResult2._2.flatMap(x => m1.get(x.name).map(y => Metric.Comparison(x, y)))
      (metricResult._1 -> r1, metricResult2._1 -> r2)
    }
  }

  implicit class PimpedMetricResultWithOutput[O](metricResult: (String, O, Seq[Metric])) {
    def andFinallyProcessResultWith[T](metricResultProcessor: MetricResultProcessor[T]): T = metricResultProcessor.processMetrics(metricResult._1 -> metricResult._3)

    def andFinallyProcessAndReturnResultWith[T](metricResultProcessor: MetricResultProcessor[T]): O = {
      metricResultProcessor.processMetrics(metricResult._1 -> metricResult._3)
      metricResult._2
    }

    def compareWith(metricResult2: (String, O, Seq[Metric])): ((String, Seq[Metric]), (String, Seq[Metric])) = {
      val m1 = metricResult._3.groupBy(_.name).view.mapValues(_.head)
      val m2 = metricResult2._3.groupBy(_.name).view.mapValues(_.head)
      val r1 = metricResult._3.flatMap(x => m2.get(x.name).map(y => Metric.Comparison(x, y)))
      val r2 = metricResult2._3.flatMap(x => m1.get(x.name).map(y => Metric.Comparison(x, y)))
      (metricResult._1 -> r1, metricResult2._1 -> r2)
    }
  }

  implicit class PimpedMetricResultPair(metricResultPair: ((String, Seq[Metric]), (String, Seq[Metric]))) {
    def andFinallyProcessResultWith[T](metricResultProcessor: MetricResultProcessor[T]): (T, T) = {
      val r1 = metricResultPair._1.andFinallyProcessResultWith(metricResultProcessor)
      val r2 = metricResultPair._2.andFinallyProcessResultWith(metricResultProcessor)
      r1 -> r2
    }
  }

}