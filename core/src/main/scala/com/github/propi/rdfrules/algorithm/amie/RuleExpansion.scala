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
  def expand(atom: Atom, support: Int, supportIncreaseRatio: Float): ExpandingRule = {
    val headDangling = if (atom.subject == dangling || atom.`object` == dangling) List(dangling) else Nil
    val (secondDanglings, others) = rule match {
      case rule: ClosedRule => Nil -> rule.variables
      case rule: DanglingRule =>
        val (danglings, others) = rule.danglings.partition(x => x != atom.subject && x != atom.`object`)
        danglings -> (others ::: rule.others)
    }
    val allDanglings = headDangling ::: secondDanglings
    if (allDanglings.isEmpty) {
      ClosedRule(atom +: rule.body, rule.head, support, supportIncreaseRatio, rule.headSize, rule.headSupport, others)
    } else {
      DanglingRule(atom +: rule.body, rule.head, support, supportIncreaseRatio, rule.headSize, rule.headSupport, allDanglings, others)
    }
    /*(atom.subject, atom.`object`) match {
      case (sv: Atom.Variable, ov: Atom.Variable) => if (sv == dangling || ov == dangling) {
        rule match {
          case rule: DanglingRule => rule.variables match {
            case ExpandingRule.OneDangling(originalDangling, others) =>
              //(d, c) | (a, c) (a, b) (a, b) => OneDangling(c) -> OneDangling(d)
              if (sv != originalDangling && ov != originalDangling) {
                DanglingRule(atom +: rule.body, rule.head, support, rule.headSize, ExpandingRule.TwoDanglings(dangling, originalDangling, others), dangling)
              } else {
                DanglingRule(atom +: rule.body, rule.head, support, rule.headSize, ExpandingRule.OneDangling(dangling, originalDangling :: others), dangling)
              }
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
            //(c, b) | (a, c) (a, b) => TwoDanglings(c, b) -> ClosedRule
            rule.variables match {
              case ExpandingRule.OneDangling(originalDangling, others) =>
                //(d, c) | (a, c) (a, b) (a, b) => OneDangling(c) -> OneDangling(d)
                if (sv != originalDangling && ov != originalDangling) {
                  DanglingRule(atom +: rule.body, rule.head, support, rule.headSize, ExpandingRule.OneDangling(originalDangling, others), originalDangling)
                } else {
                  ClosedRule(atom +: rule.body, rule.head, support, rule.headSize, originalDangling :: rule.variables.others, originalDangling)
                }
              case ExpandingRule.TwoDanglings(dangling1, dangling2, others) =>
                //(d, c) | (a, c) (a, b) => TwoDanglings(c, b) -> TwoDanglings(d, b)
                val rem = Set(dangling1, dangling2) - sv - ov
                if (rem.size == 2) {
                  DanglingRule(atom +: rule.body, rule.head, support, rule.headSize, ExpandingRule.TwoDanglings(dangling1, dangling2, others), dangling1)
                } else if (rem.size == 1) {
                  val (pastDangling, dangling) = if (rem(dangling1)) (dangling2, dangling1) else (dangling1, dangling2)
                  DanglingRule(atom +: rule.body, rule.head, support, rule.headSize, ExpandingRule.OneDangling(dangling, pastDangling :: others), dangling)
                } else {
                  ClosedRule(atom +: rule.body, rule.head, support, rule.headSize, dangling1 :: dangling2 :: rule.variables.others, dangling1)
                }
            }
        }
      }
      case (s, o) => rule match {
        case rule: ClosedRule =>
          //(a, C) | (a, b) (a, b) => ClosedRule -> ClosedRule
          ClosedRule(atom +: rule.body, rule.head, support, rule.headSize, rule.variables, rule.maxVariable)
        case rule: DanglingRule => rule.variables match {
          case ExpandingRule.OneDangling(dangling, others) =>
            //(c, C) | (a, c) (a, b) (a, b) => OneDangling(c) -> ClosedRule
            if (s == dangling || o == dangling) {
              ClosedRule(atom +: rule.body, rule.head, support, rule.headSize, dangling :: others, dangling)
            } else {
              DanglingRule(atom +: rule.body, rule.head, support, rule.headSize, ExpandingRule.OneDangling(dangling, others), dangling)
            }
          case ExpandingRule.TwoDanglings(dangling1, dangling2, others) =>
            //(c, C) | (a, c) (a, b) => TwoDanglings(c, b) -> OneDangling(b)
            val rem = Set[Atom.Item](dangling1, dangling2) - s - o
            if (rem.size == 2) {
              DanglingRule(atom +: rule.body, rule.head, support, rule.headSize, ExpandingRule.TwoDanglings(dangling1, dangling2, others), dangling1)
            } else {
              val (pastDangling, dangling) = if (rem(dangling1)) (dangling2, dangling1) else (dangling1, dangling2)
              DanglingRule(atom +: rule.body, rule.head, support, rule.headSize, ExpandingRule.OneDangling(dangling, pastDangling :: others), dangling)
            }
        }
      }
    }*/
  }

}
