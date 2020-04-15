package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.data.Triple
import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.extensions.TraversableOnceExtension._

/**
  * Created by Vaclav Zeman on 10. 4. 2020.
  */
trait ClusterDistancesTaskPostprocessor extends TaskPostProcessor[Ruleset, Seq[Metric]] {

  private def computeIntraClusterSimilarity(ruleset: Ruleset): Double = {
    if (ruleset.size > 1) {
      val tripleMap = collection.mutable.Map.empty[Triple, Boolean]
      for {
        rule <- ruleset.coveredTriples()
        triple <- rule.graph.triples
      } {
        tripleMap.get(triple) match {
          case Some(true) =>
          case Some(false) => tripleMap.update(triple, true)
          case None => tripleMap.update(triple, false)
        }
      }
      val rulesStats = ruleset.coveredTriples().view.map { rule =>
        rule.graph.triples.foldLeft((0, 0)) {
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
      println("CLUSTER ****")
      ruleset.foreach(println)
      println("***** total & intersections")
      rulesStats.foreach(println)
      println("***** stats")
      println(s"maxL: $maxL, $num / $den = ${num / den}")
      num / den
    } else {
      1.0
    }
  }

  private def computeInterClustersSimilarity(cluster1: Ruleset, cluster2: Ruleset): Double = {
    val tripleSet = cluster1.coveredTriples(distinct = false).view.flatMap(_.graph.triples).toSet
    val (clusterLen2, intersections) = cluster2.coveredTriples(distinct = false).view.flatMap(_.graph.triples).distinct.foldLeft((0, 0)) { case ((total, intersections), triple) =>
      (total + 1) -> (intersections + (if (tripleSet(triple)) 1 else 0))
    }
    println("***** INTER BETWEEN")
    println("CLUSTER 1")
    cluster1.foreach(println)
    println("CLUSTER 2")
    cluster2.foreach(println)
    println(s"stats max: $intersections / ${tripleSet.size}, $intersections / $clusterLen2")
    math.min(intersections.toDouble / tripleSet.size, intersections.toDouble / clusterLen2)
  }

  protected def postProcess(result: Ruleset): Seq[Metric] = {
    result.sorted.foreach(println)
    val numberOfRules = Metric.Number("rules", result.size)
    val clusters = result.rules.view.map(_.measures[Measure.Cluster].number).toSet.toIndexedSeq
    val numberOfClusters = Metric.Number("clusters", clusters.size)
    val interCache = collection.mutable.Map.empty[Set[Int], Double]
    val clustersSimilarities = for (cluster <- clusters) yield {
      val clusterRuleset = result.filter(_.measures[Measure.Cluster].number == cluster)
      val intraClusterSimilarity = computeIntraClusterSimilarity(clusterRuleset)
      for (yCluster <- clusters) yield {
        if (cluster == yCluster) {
          intraClusterSimilarity
        } else {
          interCache.getOrElseUpdate(
            Set(cluster, yCluster),
            computeInterClustersSimilarity(clusterRuleset, result.filter(_.measures[Measure.Cluster].number == yCluster))
          )
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