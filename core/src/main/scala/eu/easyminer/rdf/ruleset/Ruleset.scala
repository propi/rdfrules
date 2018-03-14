package eu.easyminer.rdf.ruleset

import eu.easyminer.rdf.data.TripleItem
import eu.easyminer.rdf.rule.Rule
import eu.easyminer.rdf.ruleset.Ruleset.RulesetView

import scala.collection.TraversableView

/**
  * Created by Vaclav Zeman on 6. 10. 2017.
  */
class Ruleset(rules: RulesetView, ord: Option[Ordering[Rule]])(implicit item: Int => TripleItem) {

  def withSorting(ord: Ordering[Rule]) = new Ruleset(rules, Some(ord))

  def withFilter(f: Rule => Boolean) = new Ruleset(rules.filter(f), ord)

}

object Ruleset {

  type RulesetView = TraversableView[Rule, Traversable[_]]

}
