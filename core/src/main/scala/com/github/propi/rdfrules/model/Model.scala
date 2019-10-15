package com.github.propi.rdfrules.model

import com.github.propi.rdfrules.algorithm.amie.AtomCounting
import com.github.propi.rdfrules.data.{Dataset, Graph, Triple}
import com.github.propi.rdfrules.index.{CompressedQuad, Index, TripleHashIndex, TripleItemHashIndex}
import com.github.propi.rdfrules.rule.Atom
import com.github.propi.rdfrules.ruleset.ResolvedRule
import com.github.propi.rdfrules.utils.Debugger

import scala.util.Try

/**
  * Created by Vaclav Zeman on 14. 10. 2019.
  */
class Model private(val rules: Traversable[ResolvedRule]) {

  def completeGraph(graph: Graph)(implicit debugger: Debugger = Debugger.EmptyDebugger): PredictionResult = completeIndex(graph.index())

  def completeDataset(dataset: Dataset)(implicit debugger: Debugger = Debugger.EmptyDebugger): PredictionResult = completeIndex(dataset.index())

  def completeIndex(index: Index): PredictionResult = {
    PredictionResult(
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
                Try(atomCounting.selectDistinctPairs(ruleBody, headVars).map(constantsToQuad).map(_.toTriple).map(PredictedTriple(_)(rule.measures)).foreach(f))
              }
            }
          }
        }
      },
      index
    )
  }

}

object Model {

  def apply(rules: Traversable[ResolvedRule]): Model = new Model(rules)

}