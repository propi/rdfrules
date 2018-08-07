package com.github.propi.rdfrules.java.ruleset;

import com.github.propi.rdfrules.algorithm.dbscan.DbScan$;
import com.github.propi.rdfrules.java.OrderingWrapper;
import com.github.propi.rdfrules.java.ReadersWriters;
import com.github.propi.rdfrules.java.ScalaConverters;
import com.github.propi.rdfrules.java.algorithm.Debugger;
import com.github.propi.rdfrules.java.algorithm.SimilarityCounting;
import com.github.propi.rdfrules.java.data.Cacheable;
import com.github.propi.rdfrules.java.data.Transformable;
import com.github.propi.rdfrules.java.index.Index;
import com.github.propi.rdfrules.java.index.Sortable;
import com.github.propi.rdfrules.java.rule.Measure;
import com.github.propi.rdfrules.java.rule.ResolvedRule;
import com.github.propi.rdfrules.java.rule.Rule;
import com.github.propi.rdfrules.java.rule.RulePattern;
import com.github.propi.rdfrules.ruleset.Ruleset$;
import scala.collection.JavaConverters;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
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

    public static Ruleset fromCache(File file, Index index) {
        return new Ruleset(Ruleset$.MODULE$.fromCache(index.asScala(), file));
    }

    public static Ruleset fromCache(String file, Index index) {
        return new Ruleset(Ruleset$.MODULE$.fromCache(index.asScala(), file));
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

    public Ruleset filterResolver(Predicate<ResolvedRule> f) {
        return asJava(asScala().filterResolved(x -> f.test(new ResolvedRule(x))));
    }

    public Ruleset sortBy(Measure.Key measure, Measure.Key... measures) {
        return asJava(asScala().sortBy(measure.getKey(), ScalaConverters.toIterable(Arrays.asList(measures), Measure.Key::getKey).toSeq()));
    }

    public <T> Ruleset sortByResolved(Function<ResolvedRule, T> f, Comparator<T> comparator) {
        return asJava(asScala().sortByResolved(v1 -> f.apply(new ResolvedRule(v1)), new OrderingWrapper<>(comparator)));
    }

    public Ruleset sortByRuleLength(Measure.Key... measures) {
        return asJava(asScala().sortByRuleLength(ScalaConverters.toIterable(Arrays.asList(measures), Measure.Key::getKey).toSeq()));
    }

    public void forEach(Consumer<ResolvedRule> consumer) {
        asScala().foreach(v1 -> {
            consumer.accept(new ResolvedRule(v1));
            return null;
        });
    }

    public ResolvedRule headResolved() {
        return asScala().headResolvedOption().map(ResolvedRule::new).getOrElse(() -> null);
    }

    public ResolvedRule findResolved(Predicate<ResolvedRule> f) {
        return asScala().findResolved(x -> f.test(new ResolvedRule(x))).map(ResolvedRule::new).getOrElse(() -> null);
    }

    public Ruleset graphBasedRules() {
        return asJava(asScala().graphBasedRules());
    }

    public Ruleset computeConfidence(double minConfidence) {
        return asJava(ruleset.computeConfidence(minConfidence));
    }

    public Ruleset computePcaConfidence(double minPcaConfidence) {
        return asJava(ruleset.computePcaConfidence(minPcaConfidence));
    }

    public Ruleset computeLift(double minConfidence) {
        return asJava(ruleset.computeLift(minConfidence));
    }

    public Ruleset computeLift() {
        return asJava(ruleset.computeLift(ruleset.computeLift$default$1()));
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
    public Ruleset makeClusters(int minNeighbours, double minSimilarity, SimilarityCounting similarityCounting, Debugger debugger) {
        return new Ruleset(ruleset.makeClusters(DbScan$.MODULE$.apply(
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
    public Ruleset makeClusters(int minNeighbours, double minSimilarity, SimilarityCounting similarityCounting) {
        return new Ruleset(ruleset.makeClusters(DbScan$.MODULE$.apply(
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
     * similarityCounting = DEFAULT
     * debugger = empty
     *
     * @return new Ruleset with counted clusters
     */
    public Ruleset makeClusters() {
        return new Ruleset(ruleset.makeClusters(DbScan$.MODULE$.apply(
                DbScan$.MODULE$.apply$default$1(),
                DbScan$.MODULE$.apply$default$2(),
                SimilarityCounting.DEFAULT().asScala(),
                DbScan$.MODULE$.apply$default$4(DbScan$.MODULE$.apply$default$1(), DbScan$.MODULE$.apply$default$2())
        )));
    }


    public Iterable<ResolvedRule> getResolvedRules() {
        return () -> JavaConverters.asJavaIterator(ruleset.resolvedRules().toIterator().map(ResolvedRule::new));
    }

    public Ruleset findSimilar(ResolvedRule rule, int k, SimilarityCounting similarityCounting) {
        return asJava(asScala().findSimilar(rule.asScala(), k, false, similarityCounting.asScala()));
    }

    public Ruleset findSimilar(ResolvedRule rule, int k) {
        return asJava(asScala().findSimilar(rule.asScala(), k, false, SimilarityCounting.DEFAULT().asScala()));
    }

    public Ruleset findDissimilar(ResolvedRule rule, int k, SimilarityCounting similarityCounting) {
        return asJava(asScala().findDissimilar(rule.asScala(), k, similarityCounting.asScala()));
    }

    public Ruleset findDissimilar(ResolvedRule rule, int k) {
        return asJava(asScala().findDissimilar(rule.asScala(), k, SimilarityCounting.DEFAULT().asScala()));
    }

    public void export(Supplier<OutputStream> osb, RulesetWriter rulesetWriter) {
        rulesetWriter.writeToOutputStream(getResolvedRules(), osb);
    }

    public void export(File file) {
        asScala().export(file, ReadersWriters.rulesNoWriter());
    }

    public void export(String file) {
        asScala().export(file, ReadersWriters.rulesNoWriter());
    }

    public void exportToJson(File file) {
        asScala().export(file, ReadersWriters.rulesJsonWriter());
    }

    public void exportToJson(String file) {
        asScala().export(file, ReadersWriters.rulesJsonWriter());
    }

    public void exportToText(File file) {
        asScala().export(file, ReadersWriters.rulesTextWriter());
    }

    public void exportToText(String file) {
        asScala().export(file, ReadersWriters.rulesTextWriter());
    }

}