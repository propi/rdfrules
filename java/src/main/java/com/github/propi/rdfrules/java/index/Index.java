package com.github.propi.rdfrules.java.index;

import com.github.propi.rdfrules.index.Index$;
import com.github.propi.rdfrules.index.TripleHashIndex;
import com.github.propi.rdfrules.java.IndexMode;
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

    public enum Mode {
        INUSEINMEMORY(IndexMode.inUseInMemory()),
        PRESERVEDINMEMORY(IndexMode.preservedInMemory());

        final private com.github.propi.rdfrules.index.Index.Mode mode;

        Mode(com.github.propi.rdfrules.index.Index.Mode mode) {
            this.mode = mode;
        }

        public com.github.propi.rdfrules.index.Index.Mode getMode() {
            return mode;
        }
    }

    public static Index fromDataset(Dataset dataset, Mode mode, Debugger debugger) {
        return new Index(Index$.MODULE$.apply(dataset.asScala(), mode.getMode(), debugger.asScala()));
    }

    public static Index fromDataset(Dataset dataset, Mode mode) {
        return fromDataset(dataset, mode, Debugger.empty());
    }

    public static Index fromDataset(Dataset dataset) {
        return fromDataset(dataset, Mode.PRESERVEDINMEMORY);
    }

    public static Index fromDataset(Dataset dataset, Debugger debugger) {
        return fromDataset(dataset, Mode.PRESERVEDINMEMORY, debugger);
    }

    public static Index fromCache(Supplier<InputStream> isb, Mode mode, Debugger debugger) {
        return new Index(Index$.MODULE$.fromCache(isb::get, mode.getMode(), debugger.asScala()));
    }

    public static Index fromCache(Supplier<InputStream> isb, Mode mode) {
        return fromCache(isb, mode, Debugger.empty());
    }

    public static Index fromCache(Supplier<InputStream> isb) {
        return fromCache(isb, Mode.PRESERVEDINMEMORY);
    }

    public static Index fromCache(Supplier<InputStream> isb, Debugger debugger) {
        return fromCache(isb, Mode.PRESERVEDINMEMORY, debugger);
    }

    public static Index fromCache(File file, Mode mode, Debugger debugger) {
        return new Index(Index$.MODULE$.fromCache(file, mode.getMode(), debugger.asScala()));
    }

    public static Index fromCache(File file, Mode mode) {
        return fromCache(file, mode, Debugger.empty());
    }

    public static Index fromCache(File file) {
        return fromCache(file, Mode.PRESERVEDINMEMORY);
    }

    public static Index fromCache(File file, Debugger debugger) {
        return fromCache(file, Mode.PRESERVEDINMEMORY, debugger);
    }

    public static Index fromCache(String file, Mode mode, Debugger debugger) {
        return new Index(Index$.MODULE$.fromCache(file, mode.getMode(), debugger.asScala()));
    }

    public static Index fromCache(String file, Mode mode) {
        return fromCache(file, mode, Debugger.empty());
    }

    public static Index fromCache(String file) {
        return fromCache(file, Mode.PRESERVEDINMEMORY);
    }

    public static Index fromCache(String file, Debugger debugger) {
        return fromCache(file, Mode.PRESERVEDINMEMORY, debugger);
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

    public Index newIndex() {
        return new Index(index.newIndex());
    }

    public Dataset toDataset() {
        return new Dataset(index.toDataset());
    }

    public <T> T tripleItemMap(Function<TripleItemHashIndex, T> f) {
        return index.tripleItemMap(x -> f.apply(new TripleItemHashIndex(x)));
    }

    public <T> T tripleMap(Function<TripleHashIndex, T> f) {
        return index.tripleMap(f::apply);
    }

    public Ruleset mine(RulesMining rulesMining) {
        return new Ruleset(index.mine(rulesMining.asScala()));
    }

    public Index withEvaluatedLazyVals() {
        return new Index(index.withEvaluatedLazyVals());
    }

}