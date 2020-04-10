package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.data.Triple
import com.github.propi.rdfrules.model.Model
import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.ruleset.Ruleset

/**
  * Created by Vaclav Zeman on 10. 4. 2020.
  */
trait ClusterDistancesTaskPostprocessor extends TaskPostProcessor[Ruleset, Seq[Metric]] {

  private def computeIntraClusterSimilarity(ruleset: Ruleset): Double = {
    val tripleMap = collection.mutable.Map.empty[Triple, Boolean]
    for {
      rule <- ruleset.resolvedRules
      triple <- Model(List(rule)).completeIndex(ruleset.index).triples
    } {
      tripleMap.get(triple) match {
        case Some(true) =>
        case Some(false) => tripleMap.update(triple, true)
        case None => tripleMap.update(triple, false)
      }
    }
    val rulesStats = ruleset.resolvedRules.view.map { rule =>
      Model(List(rule)).completeIndex(ruleset.index).triples.foldLeft((0, 0)) {
        case ((total, intersections), triple) => (total + 1) -> (intersections + (if (tripleMap(triple)) 1 else 0))
      }
    }.toSeq
    val maxL = rulesStats.iterator.map(_._1).max
    val (num, den) = rulesStats.iterator.map { case (total, intersections) =>
      val weight = maxL.toDouble / total
      val relIntersections = intersections.toDouble / total
      (relIntersections * weight) -> weight
    }.foldLeft((0.0, 0.0)) { case ((num, den), (dNum, dDen)) =>
      (num + dNum) -> (den + dDen)
    }
    num / den
  }

  private def computeInterClustersSimilarity(cluster1: Ruleset, cluster2: Ruleset): Double = {
    val tripleSet = collection.mutable.Set.empty[Triple]
    val clusterLen1 = cluster1.model.completeIndex(cluster1.index).triples.foldLeft(0) { (total, triple) =>
      tripleSet += triple
      total + 1
    }
    val (clusterLen2, intersections) = cluster2.model.completeIndex(cluster2.index).triples.foldLeft((0, 0)) { case ((total, intersections), triple) =>
      (total + 1) -> (intersections + (if (tripleSet(triple)) 1 else 0))
    }
    math.max(intersections.toDouble / clusterLen1, intersections.toDouble / clusterLen2)
  }

  protected def postProcess(result: Ruleset): Seq[Metric] = {
    val numberOfRules = Metric.Number("rules", result.size)
    val clusters = result.rules.view.map(_.measures[Measure.Cluster].number).toSet.toIndexedSeq
    val numberOfClusters = Metric.Number("clusters", clusters.size)
    val clustersSimilarities = for (cluster <- clusters) yield {
      val clusterRuleset = result.filter(_.measures[Measure.Cluster].number == cluster)
      val intraClusterSimilarity = computeIntraClusterSimilarity(clusterRuleset)
      for (yCluster <- clusters) yield {
        if (cluster == yCluster) {
          intraClusterSimilarity
        } else {
          computeInterClustersSimilarity(clusterRuleset, result.filter(_.measures[Measure.Cluster].number == yCluster))
        }
      }
    }
    List(
      numberOfRules,
      numberOfClusters,
      Metric.ClustersSimilarities("clustersSimilarities", clustersSimilarities)
    )
  }

}