package com.github.propi.rdfrules.java.algorithm;

import com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting$;
import com.github.propi.rdfrules.java.SimilarityCountingConverters;
import com.github.propi.rdfrules.rule.Rule;
import com.github.propi.rdfrules.rule.Rule$;

/**
 * Created by Vaclav Zeman on 14. 5. 2018.
 */
public class SimilarityCounting {

    public enum RuleSimilarityCounting {
        ATOMS,
        SUPPORT,
        CONFIDENCE,
        LENGTH,
        PCA_CONFIDENCE,
        LIFT
    }

    final private com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting.Comb<Rule> similarityCounting;

    private SimilarityCounting(com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting.Comb<Rule> similarityCounting) {
        this.similarityCounting = similarityCounting;
    }

    public SimilarityCounting(RuleSimilarityCounting ruleSimilarityCounting, double weight) {
        this(SimilarityCountingConverters.toScalaRuleComb(ruleSimilarityCounting, weight));
    }

    public static SimilarityCounting DEFAULT() {
        return new SimilarityCounting(null);
    }

    public com.github.propi.rdfrules.algorithm.dbscan.SimilarityCounting<Rule.Simple> asScala() {
        if (similarityCounting == null) {
            return SimilarityCounting$.MODULE$.ruleSimpleSimilarityCounting(Rule$.MODULE$.ruleSimilarityCounting());
        }
        return SimilarityCounting$.MODULE$.ruleSimpleSimilarityCounting(SimilarityCountingConverters.toScalaRuleSimilarityCounting(similarityCounting));
    }

    public SimilarityCounting add(RuleSimilarityCounting ruleSimilarityCounting, double weight) {
        if (similarityCounting == null) {
            return new SimilarityCounting(ruleSimilarityCounting, weight);
        }
        return new SimilarityCounting(similarityCounting.$tilde(SimilarityCountingConverters.toScalaWeightedSimilarityCounting(ruleSimilarityCounting, weight)));
    }

}