package com.github.propi.rdfrules.java.ruleset;

import com.github.propi.rdfrules.java.ScalaConverters;
import com.github.propi.rdfrules.java.rule.ResolvedRule;
import com.github.propi.rdfrules.stringifier.CommonStringifiers$;

import java.io.OutputStream;
import java.util.function.Supplier;

/**
 * Created by Vaclav Zeman on 13. 5. 2018.
 */
public interface RulesetWriter {

    void writeToOutputStream(Iterable<ResolvedRule> rules, Supplier<OutputStream> osb);

    class Json implements RulesetWriter {
        @Override
        public void writeToOutputStream(Iterable<ResolvedRule> rules, Supplier<OutputStream> osb) {
            com.github.propi.rdfrules.ruleset.formats.Json.jsonRulesetWriter().writeToOutputStream(
                    ScalaConverters.toIterable(rules, ResolvedRule::asScala),
                    osb::get
            );
        }
    }

    class Text implements RulesetWriter {
        @Override
        public void writeToOutputStream(Iterable<ResolvedRule> rules, Supplier<OutputStream> osb) {
            com.github.propi.rdfrules.ruleset.formats.Text.textRulesetWriter(CommonStringifiers$.MODULE$.resolvedRuleStringifier()).writeToOutputStream(
                    ScalaConverters.toIterable(rules, ResolvedRule::asScala),
                    osb::get
            );
        }
    }

}