package com.github.propi.rdfrules.java.ruleset;

import com.github.propi.rdfrules.algorithm.dbscan.DbScan$;
import com.github.propi.rdfrules.java.ScalaConverters;
import com.github.propi.rdfrules.java.algorithm.Debugger;
import com.github.propi.rdfrules.java.algorithm.SimilarityCounting;
import com.github.propi.rdfrules.java.data.Cacheable;
import com.github.propi.rdfrules.java.data.Transformable;
import com.github.propi.rdfrules.java.index.Index;
import com.github.propi.rdfrules.java.index.Sortable;
import com.github.propi.rdfrules.java.index.TripleItemHashIndex;
import com.github.propi.rdfrules.java.rule.ResolvedRule;
import com.github.propi.rdfrules.java.rule.Rule;
import com.github.propi.rdfrules.java.rule.RulePattern;
import com.github.propi.rdfrules.ruleset.Ruleset$;
import scala.collection.JavaConverters;
import scala.runtime.BoxedUnit;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by Vaclav Zeman on 13. 5. 2018.
 */
public class Ruleset implements
        Transformable<com.github.propi.rdfrules.rule.Rule.Simple, Rule, com.github.propi.rdfrules.ruleset.Ruleset, Ruleset>,
        Sortable<com.github.propi.rdfrules.rule.Rule.Simple, Rule, com.github.propi.rdfrules.ruleset.Ruleset, Ruleset>,
        Cacheable<com.github.propi.rdfrules.rule.Rule.Simple, com.github.propi.rdfrules.ruleset.Ruleset, Ruleset> {

    final private com.github.propi.rdfrules.ruleset.Ruleset ruleset;

    public Ruleset(com.github.propi.rdfrules.ruleset.Ruleset ruleset) {
        this.ruleset = ruleset;
    }

    public static Ruleset fromCache(Supplier<InputStream> isb, Index index) {
        return new Ruleset(Ruleset$.MODULE$.fromCache(index.asScala(), isb::get));
    }

    @Override
    public com.github.propi.rdfrules.ruleset.Ruleset asScala() {
        return ruleset;
    }

    @Override
    public Rule asJavaItem(com.github.propi.rdfrules.rule.Rule.Simple x) {
        return new Rule(x);
    }

    @Override
    public com.github.propi.rdfrules.rule.Rule.Simple asScalaItem(Rule x) {
        return x.asScala();
    }

    @Override
    public Ruleset asJava(com.github.propi.rdfrules.ruleset.Ruleset scala) {
        return new Ruleset(scala);
    }

    public Index getIndex() {
        return new Index(asScala().index());
    }

    public Ruleset filter(RulePattern rulePattern, RulePattern... rulePatterns) {
        return asJava(asScala().filter(rulePattern.asScala(), ScalaConverters.toIterable(Arrays.asList(rulePatterns), RulePattern::asScala).toSeq()));
    }

    public void forEach(Consumer<Rule> consumer) {
        asScala().foreach(v1 -> {
            consumer.accept(asJavaItem(v1));
            return null;
        });
    }

    public Ruleset countConfidence(double minConfidence) {
        return asJava(ruleset.countConfidence(minConfidence));
    }

    public Ruleset countPcaConfidence(double minPcaConfidence) {
        return asJava(ruleset.countPcaConfidence(minPcaConfidence));
    }

    public Ruleset countLift(double minConfidence) {
        return asJava(ruleset.countLift(minConfidence));
    }

    public Ruleset countLift() {
        return asJava(ruleset.countLift(ruleset.countLift$default$1()));
    }

    public Ruleset countPcaLift(double minPcaConfidence) {
        return asJava(ruleset.countPcaLift(minPcaConfidence));
    }

    public Ruleset countPcaLift() {
        return asJava(ruleset.countPcaLift(ruleset.countPcaLift$default$1()));
    }

    /**
     * Count clusters
     *
     * @param minNeighbours      min neighbours created a cluster
     * @param minSimilarity      min similarity threshold to be neighbour
     * @param similarityCounting similarity counting algorithm
     * @param debugger           debugger for watching of the cluster counting progress
     * @return new Ruleset with counted clusters
     */
    public Ruleset countClusters(int minNeighbours, double minSimilarity, SimilarityCounting similarityCounting, Debugger debugger) {
        return new Ruleset(ruleset.countClusters(DbScan$.MODULE$.apply(
                minNeighbours,
                minSimilarity,
                similarityCounting.asScala(),
                debugger.asScala()
        )));
    }

    /**
     * Count clusters with empty debugger
     *
     * @param minNeighbours      min neighbours created a cluster
     * @param minSimilarity      min similarity threshold to be neighbour
     * @param similarityCounting similarity counting algorithm
     * @return new Ruleset with counted clusters
     */
    public Ruleset countClusters(int minNeighbours, double minSimilarity, SimilarityCounting similarityCounting) {
        return new Ruleset(ruleset.countClusters(DbScan$.MODULE$.apply(
                minNeighbours,
                minSimilarity,
                similarityCounting.asScala(),
                DbScan$.MODULE$.apply$default$4(minNeighbours, minSimilarity)
        )));
    }

    /**
     * Count clusters with default parameters
     * minNeighbours = 5
     * minSimilarity = 0.9
     * similarityCounting = (0.6 * AtomsSimilarityCounting) + (0.1 * LengthSimilarityCounting) + (0.15 * SupportSimilarityCounting) + (0.15 * ConfidenceSimilarityCounting)
     * debugger = empty
     *
     * @return new Ruleset with counted clusters
     */
    public Ruleset countClusters() {
        return new Ruleset(ruleset.countClusters(DbScan$.MODULE$.apply(
                DbScan$.MODULE$.apply$default$1(),
                DbScan$.MODULE$.apply$default$2(),
                SimilarityCounting.DEFAULT().asScala(),
                DbScan$.MODULE$.apply$default$4(DbScan$.MODULE$.apply$default$1(), DbScan$.MODULE$.apply$default$2())
        )));
    }

    public void useMapper(Function<TripleItemHashIndex, Consumer<Ruleset>> f) {
        ruleset.useMapper(x -> (y -> {
            f.apply(new TripleItemHashIndex(x)).accept(new Ruleset(y));
            return BoxedUnit.UNIT;
        }));
    }

    public Iterable<ResolvedRule> getResolvedRules() {
        return () -> JavaConverters.asJavaIterator(ruleset.resolvedRules().toIterator().map(ResolvedRule::new));
    }

    public void export(Supplier<OutputStream> osb, RulesetWriter rulesetWriter) {
        rulesetWriter.writeToOutputStream(getResolvedRules(), osb);
    }

}
