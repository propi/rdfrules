package com.github.propi.rdfrules.java

import com.github.propi.rdfrules.algorithm.dbscan
import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting._
import com.github.propi.rdfrules.java.algorithm.SimilarityCounting
import com.github.propi.rdfrules.rule.Rule

/**
  * Created by Vaclav Zeman on 14. 5. 2018.
  */
object SimilarityCountingConverters {

  def toScalaWeightedSimilarityCounting(x: SimilarityCounting.RuleSimilarityCounting, weight: Double): WeightedSimilarityCounting[Rule] = x match {
    case SimilarityCounting.RuleSimilarityCounting.ATOMS => weight * AtomsSimilarityCounting
    case SimilarityCounting.RuleSimilarityCounting.CONFIDENCE => weight * ConfidenceSimilarityCounting
    case SimilarityCounting.RuleSimilarityCounting.LENGTH => weight * LengthSimilarityCounting
    case SimilarityCounting.RuleSimilarityCounting.SUPPORT => weight * SupportSimilarityCounting
    case SimilarityCounting.RuleSimilarityCounting.PCA_CONFIDENCE => weight * PcaConfidenceSimilarityCounting
    case SimilarityCounting.RuleSimilarityCounting.LIFT => weight * LiftSimilarityCounting
  }

  def toScalaRuleComb(x: SimilarityCounting.RuleSimilarityCounting, weight: Double): Comb[Rule] = toScalaWeightedSimilarityCounting(x, weight)

  def toScalaRuleSimilarityCounting(x: Comb[Rule]): dbscan.SimilarityCounting[Rule.Simple] = x

}