package com.github.propi.rdfrules.java.rule;

import com.github.propi.rdfrules.java.SeqWrapper;

import java.util.List;
import java.util.Objects;

/**
 * Created by Vaclav Zeman on 13. 5. 2018.
 */
public class ResolvedRule extends RuleMeasures {

    final private com.github.propi.rdfrules.ruleset.ResolvedRule resolvedRule;

    public ResolvedRule(com.github.propi.rdfrules.ruleset.ResolvedRule resolvedRule) {
        super(resolvedRule.measures());
        this.resolvedRule = resolvedRule;
    }

    public com.github.propi.rdfrules.ruleset.ResolvedRule asScala() {
        return resolvedRule;
    }

    public ResolvedAtom getHead() {
        return new ResolvedAtom(resolvedRule.head());
    }

    public List<ResolvedAtom> getBody() {
        return new SeqWrapper<>(resolvedRule.body()).map(ResolvedAtom::new).asJava();
    }

    public int getRuleLength() {
        return resolvedRule.ruleLength();
    }

    @Override
    public String toString() {
        return resolvedRule.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolvedRule that = (ResolvedRule) o;
        return Objects.equals(resolvedRule, that.resolvedRule);
    }

    @Override
    public int hashCode() {
        return resolvedRule.hashCode();
    }

}