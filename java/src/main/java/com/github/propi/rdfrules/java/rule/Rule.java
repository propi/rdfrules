package com.github.propi.rdfrules.java.rule;

import com.github.propi.rdfrules.java.SeqWrapper;

import java.util.List;
import java.util.Objects;

/**
 * Created by Vaclav Zeman on 13. 5. 2018.
 */
public class Rule extends RuleMeasures {

    final private com.github.propi.rdfrules.rule.Rule.Simple rule;

    public Rule(com.github.propi.rdfrules.rule.Rule.Simple rule) {
        super(rule.measures());
        this.rule = rule;
    }

    public com.github.propi.rdfrules.rule.Rule.Simple asScala() {
        return rule;
    }

    public Atom getHead() {
        return new Atom(rule.head());
    }

    public List<Atom> getBody() {
        return new SeqWrapper<>(rule.body()).map(Atom::new).asJava();
    }

    public int getRuleLength() {
        return rule.ruleLength();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Rule rule1 = (Rule) o;
        return Objects.equals(rule, rule1.rule);
    }

    @Override
    public int hashCode() {
        return rule.hashCode();
    }

    @Override
    public String toString() {
        return rule.toString();
    }

}