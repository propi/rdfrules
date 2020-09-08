package com.github.propi.rdfrules.java.index;

import com.github.propi.rdfrules.index.Index$;
import com.github.propi.rdfrules.index.TripleIndex;
import com.github.propi.rdfrules.index.TripleItemIndex;
import com.github.propi.rdfrules.java.algorithm.Debugger;
import com.github.propi.rdfrules.java.algorithm.RulesMining;
import com.github.propi.rdfrules.java.data.Dataset;
import com.github.propi.rdfrules.java.ruleset.Ruleset;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by Vaclav Zeman on 11. 5. 2018.
 */
public class Index {

    final private com.github.propi.rdfrules.index.Index index;

    public Index(com.github.propi.rdfrules.index.Index index) {
        this.index = index;
    }

    public static Index fromDataset(Dataset dataset, Debugger debugger) {
        return new Index(Index$.MODULE$.apply(dataset.asScala(), false, debugger.asScala()));
    }

    public static Index fromDataset(Dataset dataset) {
        return fromDataset(dataset, Debugger.empty());
    }


    public static Index fromCache(Supplier<InputStream> isb, Debugger debugger) {
        return new Index(Index$.MODULE$.fromCache(isb::get, false, debugger.asScala()));
    }

    public static Index fromCache(Supplier<InputStream> isb) {
        return fromCache(isb, Debugger.empty());
    }


    public static Index fromCache(File file, Debugger debugger) {
        return new Index(Index$.MODULE$.fromCache(file, false, debugger.asScala()));
    }

    public static Index fromCache(File file) {
        return fromCache(file, Debugger.empty());
    }

    public static Index fromCache(String file) {
        return fromCache(file, Debugger.empty());
    }

    public static Index fromCache(String file, Debugger debugger) {
        return fromCache(new File(file), debugger);
    }

    public com.github.propi.rdfrules.index.Index asScala() {
        return index;
    }

    public void cache(Supplier<OutputStream> osb) {
        index.cache(osb::get);
    }

    public void cache(File file) {
        index.cache(file);
    }

    public void cache(String file) {
        index.cache(file);
    }

    public Dataset toDataset() {
        return new Dataset(index.toDataset());
    }

    public <T> T tripleItemMap(Function<TripleItemIndex, T> f) {
        return index.tripleItemMap(f::apply);
    }

    public <T> T tripleMap(Function<TripleIndex, T> f) {
        return index.tripleMap(f::apply);
    }

    public Ruleset mine(RulesMining rulesMining) {
        return new Ruleset(index.mine(rulesMining.asScala()));
    }

    public Index withEvaluatedLazyVals() {
        return new Index(index.withEvaluatedLazyVals());
    }

}