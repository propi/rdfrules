package com.github.propi.rdfrules.model

import com.github.propi.rdfrules.algorithm.amie.{Amie, AtomCounting}
import com.github.propi.rdfrules.data.{Dataset, Graph, TripleItem}
import com.github.propi.rdfrules.index.{CompressedQuad, Index, TripleHashIndex, TripleItemHashIndex}
import com.github.propi.rdfrules.rule.{Atom, AtomPattern, RuleConstraint, RulePattern, Threshold}
import com.github.propi.rdfrules.ruleset.ResolvedRule
import com.github.propi.rdfrules.utils.Debugger

import scala.util.Try

/**
  * Created by Vaclav Zeman on 14. 10. 2019.
  */
class Model private(val rules: Traversable[ResolvedRule]) {

  def completeGraph(graph: Graph)(implicit debugger: Debugger = Debugger.EmptyDebugger): PredictionResult = completeIndex(graph.index())

  def completeDataset(dataset: Dataset)(implicit debugger: Debugger = Debugger.EmptyDebugger): PredictionResult = completeIndex(dataset.index())

  def completeIndex(index: Index): PredictionResult = PredictionResult(
    new Traversable[PredictedTriple] {
      def foreach[U](f: PredictedTriple => U): Unit = {
        index.tripleItemMap { mapper =>
          index.tripleMap { implicit thi =>
            val atomCounting = new AtomCounting {
              implicit val tripleIndex: TripleHashIndex = thi
            }
            rules.view.map(ResolvedRule.simple(_)(mapper)).foreach { case (rule, ruleMapper) =>
              implicit val mapper2: TripleItemHashIndex = mapper.extendWith(ruleMapper)
              val ruleBody = rule.body.toSet
              val headVars = List(rule.head.subject, rule.head.`object`).collect {
                case x: Atom.Variable => x
              }
              val constantsToQuad: Seq[Atom.Constant] => CompressedQuad = (rule.head.subject, rule.head.`object`) match {
                case (_: Atom.Variable, _: Atom.Variable) => constants => CompressedQuad(constants.head.value, rule.head.predicate, constants.last.value, 0)
                case (_: Atom.Variable, Atom.Constant(o)) => constants => CompressedQuad(constants.head.value, rule.head.predicate, o, 0)
                case (Atom.Constant(s), _: Atom.Variable) => constants => CompressedQuad(s, rule.head.predicate, constants.head.value, 0)
                case (Atom.Constant(s), Atom.Constant(o)) => _ => CompressedQuad(s, rule.head.predicate, o, 0)
              }
              Try(atomCounting.selectDistinctPairs(ruleBody, headVars).map(constantsToQuad).map(_.toTriple).map(PredictedTriple(_)(rule)).foreach(f))
            }
          }
        }
      }
    },
    index
  )

}

object Model {

  def apply(rules: Traversable[ResolvedRule]): Model = new Model(rules)

  def main(args: Array[String]): Unit = {
    val ids = Graph("workspace/trains.sql").filter(_.predicate.hasSameUriAs("train_id")).triples.toVector.groupBy(_.`object`).toVector
    println(ids.size)
    val (train, test) = {
      val (train, test) = ids.splitAt((ids.length * 0.6).toInt)
      train.flatMap(_._2).flatMap(x => List(x.subject, x.`object`)).toSet -> test.flatMap(_._2).flatMap(x => List(x.subject, x.`object`)).toSet
    }
    /*println(train)
    println("**********")
    println(test)*/
    val traingGraph = Graph("workspace/trains.sql").filter(x => train(x.subject))
    val testGraph = Graph("workspace/trains.sql").filter(x => test(x.subject))
    val rules = traingGraph.mine(Amie().addThreshold(Threshold.MinHeadSize(1)).addThreshold(Threshold.MaxRuleLength(4)).addThreshold(Threshold.TopK(10000)).addConstraint(RuleConstraint.WithInstances(false)).addPattern(RulePattern(AtomPattern(predicate = TripleItem.Uri("direction")))))
    rules.computeConfidence(0.5).sorted.take(100).resolvedRules.foreach(println)
    //val cg = rules.computeConfidence(0.5).sorted.model.completeGraph(testGraph).onlyFunctionalProperties
    //val pt = cg.predictedTriples.toVector
    /*pt.foreach { p =>
      println(s"$p, ${p.rule}")
    }
    println(pt.size)
    println(cg.evaluate)*/
  }

}