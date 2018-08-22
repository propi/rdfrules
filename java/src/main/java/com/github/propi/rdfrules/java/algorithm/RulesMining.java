package com.github.propi.rdfrules.java.algorithm;

import com.github.propi.rdfrules.algorithm.amie.Amie$;
import com.github.propi.rdfrules.java.RulesMiningWrapper;
import com.github.propi.rdfrules.java.data.TripleItem;
import com.github.propi.rdfrules.java.rule.RulePattern;
import com.github.propi.rdfrules.rule.RuleConstraint;
import com.github.propi.rdfrules.rule.Threshold;

import java.util.Set;

/**
 * Created by Vaclav Zeman on 13. 5. 2018.
 */
public class RulesMining {

    final private com.github.propi.rdfrules.algorithm.RulesMining rulesMining;

    public RulesMining(com.github.propi.rdfrules.algorithm.RulesMining rulesMining) {
        this.rulesMining = rulesMining;
    }

    public static RulesMining amie(Debugger debugger) {
        return new RulesMining(Amie$.MODULE$.apply(debugger.asScala()));
    }

    public static RulesMining amie() {
        return new RulesMining(Amie$.MODULE$.apply(Debugger.empty().asScala()));
    }

    public com.github.propi.rdfrules.algorithm.RulesMining asScala() {
        return rulesMining;
    }

    public RulesMining withMinHeadSize(int minHeadSize) {
        return new RulesMining(rulesMining.addThreshold(new Threshold.MinHeadSize(minHeadSize)));
    }

    public RulesMining withMinHeadCoverage(double minHeadCoverage) {
        return new RulesMining(rulesMining.addThreshold(new Threshold.MinHeadCoverage(minHeadCoverage)));
    }

    public RulesMining withMaxRuleLength(int maxRuleLength) {
        return new RulesMining(rulesMining.addThreshold(new Threshold.MaxRuleLength(maxRuleLength)));
    }

    public RulesMining withTopK(int topK) {
        return new RulesMining(rulesMining.addThreshold(new Threshold.TopK(topK)));
    }

    public RulesMining withTimeout(int timeout) {
        return new RulesMining(rulesMining.addThreshold(new Threshold.Timeout(timeout)));
    }

    public RulesMining withOnlyPredicates(Set<TripleItem.Uri> predicates) {
        return new RulesMining(new RulesMiningWrapper(rulesMining).withOnlyPredicates(predicates));
    }

    public RulesMining withoutPredicates(Set<TripleItem.Uri> predicates) {
        return new RulesMining(new RulesMiningWrapper(rulesMining).withoutPredicates(predicates));
    }

    public RulesMining withInstances(boolean onlyObjects) {
        return new RulesMining(rulesMining.addConstraint(new RuleConstraint.WithInstances(onlyObjects)));
    }

    public RulesMining withoutDuplicitPredicates() {
        return new RulesMining(rulesMining.addConstraint(new RuleConstraint.WithoutDuplicitPredicates()));
    }

    public RulesMining addPattern(RulePattern rulePattern) {
        return new RulesMining(rulesMining.addPattern(rulePattern.asScala()));
    }

}