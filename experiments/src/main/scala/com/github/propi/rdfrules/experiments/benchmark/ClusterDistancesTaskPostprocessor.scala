package com.github.propi.rdfrules.experiments.benchmark

import com.github.propi.rdfrules.index.CompressedTriple
import com.github.propi.rdfrules.rule.Measure
import com.github.propi.rdfrules.ruleset.Ruleset
import com.github.propi.rdfrules.utils.extensions.TraversableOnceExtension._

import scala.language.postfixOps

/**
  * Created by Vaclav Zeman on 10. 4. 2020.
  */
trait ClusterDistancesTaskPostprocessor extends TaskPostProcessor[Ruleset, Seq[Metric]] {

  private class MutableWeight {
    private var i = 0
    private var weight = 0

    def weight(maxWeight: Int): Int = ((maxWeight * i) - weight) + 1

    def intersections: Int = i

    def +=(x: Int): Unit = {
      i += 1
      weight += x
    }
  }

  private def computeIntraClusterSimilarity(ruleset: Ruleset): Double = {
    if (ruleset.size > 1) {
      val tripleMap = collection.mutable.Map.empty[CompressedTriple, MutableWeight]
      var maxWeight = 0
      for {
        coveredPaths <- ruleset.coveredPaths()
      } {
        var i = 0
        val triplesWeights = collection.mutable.ListBuffer.empty[MutableWeight]
        for (compressedTriple <- coveredPaths.triples(true)) {
          i += 1
          triplesWeights += tripleMap.getOrElseUpdate(compressedTriple, new MutableWeight)
        }
        for (tripleWeight <- triplesWeights) {
          tripleWeight += i
        }
        maxWeight = math.max(maxWeight, i)
      }
      val sumWeights = tripleMap.valuesIterator.map(_.weight(maxWeight).toLong).sum
      /*println(s"max: $maxWeight, sum: $sumWeights")
      println("-----")
      tripleMap.valuesIterator.foreach(x => println(s"(${x.intersections.toDouble} / ${ruleset.size}) * ${x.weight(maxWeight)}"))
      println("*********")
      println("*********")
      println("*********")*/
      tripleMap.valuesIterator.map(x => (x.intersections.toDouble / ruleset.size) * x.weight(maxWeight)).sum / sumWeights
      /*val rulesStats = ruleset.coveredPaths().view.map { rule =>
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
      }*/
      /*println("CLUSTER ****")
      ruleset.foreach(println)
      println("***** total & intersections")
      rulesStats.foreach(println)
      println("***** stats")
      println(s"maxL: $maxL, $num / $den = ${num / den}")*/
      //num / den
    } else {
      1.0
    }
  }

  private def computeInterClustersSimilarity(cluster1: Ruleset, cluster2: Ruleset): Double = {
    val tripleSet = cluster1.coveredPaths().view.flatMap(_.triples(false)).toSet
    val (clusterLen2, intersections) = cluster2.coveredPaths().view.flatMap(_.triples(false)).distinct.foldLeft((0, 0)) { case ((total, intersections), triple) =>
      (total + 1) -> (intersections + (if (tripleSet(triple)) 1 else 0))
    }
    /*println("***** INTER BETWEEN")
    println("CLUSTER 1")
    cluster1.foreach(println)
    println("CLUSTER 2")
    cluster2.foreach(println)*/
    //println(s"stats max: $intersections / ${tripleSet.size}, $intersections / $clusterLen2")
    math.max(intersections.toDouble / tripleSet.size, intersections.toDouble / clusterLen2)
  }

  protected def postProcess(result: Ruleset): Seq[Metric] = {
    //result.sorted.foreach(println)
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