package com.github.propi.rdfrules.experiments.benchmark

/**
  * Created by Vaclav Zeman on 19. 5. 2019.
  */
trait MetricsAggregator {

  def aggregateMetrics(metricsSeq: Seq[Seq[Metric]]): Seq[Metric]

}

object MetricsAggregator {

  object StatsAggregator extends MetricsAggregator {
    def aggregateMetrics(metricsSeq: Seq[Seq[Metric]]): Seq[Metric] = metricsSeq.flatten.groupBy(_.name).view.mapValues { variables =>
      val col = variables.collect {
        case x: Metric.Simple => x
      }
      val max = col.maxBy(_.doubleValue)
      val min = col.minBy(_.doubleValue)
      val avg = col.reduceLeft(_ + _) / col.length
      val variance = col.map(x => math.pow(x.doubleValue - avg.doubleValue, 2)).sum / col.length
      val stdDev = math.sqrt(variance)
      Metric.Stats(avg, avg.update(stdDev), min, max)
    }.values.toSeq
  }

}