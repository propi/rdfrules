package com.github.propi.rdfrules.algorithm.amie

import com.github.propi.rdfrules.rule.ExpandingRule.{ClosedRule, DanglingRule}
import com.github.propi.rdfrules.rule.{Atom, ExpandingRule}

/**
  * Created by Vaclav Zeman on 15. 3. 2018.
  */
trait RuleExpansion {

  val dangling: Atom.Variable
  val rule: ExpandingRule

  /**
    * Create new rule from this rule with added atom
    * It creates three variants: ClosedRule, OneDanglingRule, TwoDanglingsRule
    *
    * @param atom    new atom which will be added to this rule
    * @param support support of this rule with new atom
    * @return extended rule with new atom
    */
  def expand(atom: Atom, support: Int): ExpandingRule = {
    (atom.subject, atom.`object`) match {
      case (sv: Atom.Variable, ov: Atom.Variable) => if (sv == dangling || ov == dangling) {
        rule match {
          case rule: DanglingRule => rule.variables match {
            case ExpandingRule.OneDangling(originalDangling, others) =>
              //(d, c) | (a, c) (a, b) (a, b) => OneDangling(c) -> OneDangling(d)
              DanglingRule(atom +: rule.body, rule.head, support, rule.headSize, ExpandingRule.OneDangling(dangling, originalDangling :: others), dangling)
            case ExpandingRule.TwoDanglings(dangling1, dangling2, others) =>
              //(d, c) | (a, c) (a, b) => TwoDanglings(c, b) -> TwoDanglings(d, b)
              val (pastDangling, secondDangling) = if (sv == dangling1 || ov == dangling1) (dangling1, dangling2) else (dangling2, dangling1)
              DanglingRule(atom +: rule.body, rule.head, support, rule.headSize, ExpandingRule.TwoDanglings(dangling, secondDangling, pastDangling :: others), dangling)
          }
          case rule: ClosedRule =>
            //(c, a) | (a, b) (a, b) => ClosedRule -> OneDangling(c)
            DanglingRule(atom +: rule.body, rule.head, support, rule.headSize, ExpandingRule.OneDangling(dangling, rule.variables), dangling)
        }
      } else {
        rule match {
          case rule: ClosedRule =>
            //(a, b) | (a, b) (a, b) => ClosedRule -> ClosedRule
            ClosedRule(atom +: rule.body, rule.head, support, rule.headSize, rule.variables, rule.maxVariable)
          case rule: DanglingRule =>
            //(c, a) | (c, a) (a, b) (a, b) => OneDangling(c) -> ClosedRule
            //(c, b) |(a, c) (a, b) => TwoDanglings(c, b) -> ClosedRule
            ClosedRule(atom +: rule.body, rule.head, support, rule.headSize, rule.variables.danglings ::: rule.variables.others, rule.maxVariable)
        }
      }
      case (_: Atom.Variable, _: Atom.Constant) | (_: Atom.Constant, _: Atom.Variable) => rule match {
        case rule: ClosedRule =>
          //(a, C) | (a, b) (a, b) => ClosedRule -> ClosedRule
          ClosedRule(atom +: rule.body, rule.head, support, rule.headSize, rule.variables, rule.maxVariable)
        case rule: DanglingRule => rule.variables match {
          case ExpandingRule.OneDangling(dangling, others) =>
            //(c, C) | (a, c) (a, b) (a, b) => OneDangling(c) -> ClosedRule
            ClosedRule(atom +: rule.body, rule.head, support, rule.headSize, dangling :: others, dangling)
          case ExpandingRule.TwoDanglings(dangling1, dangling2, others) =>
            //(c, C) | (a, c) (a, b) => TwoDanglings(c, b) -> OneDangling(b)
            val (pastDangling, dangling) = if (atom.subject == dangling1 || atom.`object` == dangling1) (dangling1, dangling2) else (dangling2, dangling1)
            DanglingRule(atom +: rule.body, rule.head, support, rule.headSize, ExpandingRule.OneDangling(dangling, pastDangling :: others), rule.maxVariable)
        }
      }
      case _ => throw new IllegalStateException
    }
  }

}
