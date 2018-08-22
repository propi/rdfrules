package com.github.propi.rdfrules.java.data;

import com.github.propi.rdfrules.java.ReadersWriters;
import com.github.propi.rdfrules.java.ScalaConverters;
import com.github.propi.rdfrules.java.algorithm.Debugger;
import com.github.propi.rdfrules.java.algorithm.RulesMining;
import com.github.propi.rdfrules.java.index.Index;
import com.github.propi.rdfrules.java.ruleset.Ruleset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;
import scala.collection.JavaConverters;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by Vaclav Zeman on 10. 5. 2018.
 */
public class Dataset implements
        Transformable<com.github.propi.rdfrules.data.Quad, Quad, com.github.propi.rdfrules.data.Dataset, Dataset>,
        Cacheable<com.github.propi.rdfrules.data.Quad, com.github.propi.rdfrules.data.Dataset, Dataset>,
        QuadsOps<com.github.propi.rdfrules.data.Dataset, Dataset>,
        Discretizable<com.github.propi.rdfrules.data.Dataset, Dataset>,
        TriplesOps {

    final private com.github.propi.rdfrules.data.Dataset dataset;

    public Dataset(com.github.propi.rdfrules.data.Dataset dataset) {
        this.dataset = dataset;
    }

    public static Dataset empty() {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.apply());
    }

    public static Dataset fromTsv(Supplier<InputStream> isb) {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.apply(isb::get, ReadersWriters.tsvReader()));
    }

    public static Dataset fromTsv(File file) {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.apply(file, ReadersWriters.tsvReader()));
    }

    public static Dataset fromTsv(String file) {
        return fromTsv(new File(file));
    }

    public static Dataset fromRdfLang(Supplier<InputStream> isb, Lang lang) {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.apply(isb::get, ReadersWriters.jenaLangReader(lang)));
    }

    public static Dataset fromRdfLang(File file, Lang lang) {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.apply(file, ReadersWriters.jenaLangReader(lang)));
    }

    public static Dataset fromRdfLang(String file, Lang lang) {
        return fromRdfLang(new File(file), lang);
    }

    public static Dataset fromFile(File file) {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.apply(file, ReadersWriters.noRdfReader()));
    }

    public static Dataset fromFile(String file) {
        return fromFile(new File(file));
    }

    public static Dataset fromQuads(Iterable<Quad> quads) {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.apply(ScalaConverters.toIterable(quads, Quad::asScala)));
    }

    public static Dataset fromGraph(Graph graph) {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.apply(graph.asScala()));
    }

    public static Dataset fromCache(Supplier<InputStream> isb) {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.fromCache(isb::get));
    }

    public static Dataset fromCache(File file) {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.fromCache(file));
    }

    public static Dataset fromCache(String file) {
        return fromCache(new File(file));
    }

    @Override
    public com.github.propi.rdfrules.data.Dataset asScala() {
        return dataset;
    }

    @Override
    public Dataset asJava(com.github.propi.rdfrules.data.Dataset scala) {
        return new Dataset(scala);
    }

    @Override
    public Quad asJavaItem(com.github.propi.rdfrules.data.Quad x) {
        return new Quad(x);
    }

    @Override
    public com.github.propi.rdfrules.data.Quad asScalaItem(Quad x) {
        return x.asScala();
    }

    public Iterable<Graph> toGraphs() {
        return () -> JavaConverters.asJavaIterator(asScala().toGraphs().toIterator().map(Graph::new));
    }

    public Dataset add(Graph graph) {
        return asJava(asScala().$plus(graph.asScala()));
    }

    public Dataset add(Dataset dataset) {
        return asJava(asScala().$plus(dataset.asScala()));
    }

    public void forEach(Consumer<Quad> consumer) {
        asScala().quads().foreach(v1 -> {
            consumer.accept(asJavaItem(v1));
            return null;
        });
    }

    public void export(Supplier<OutputStream> osb, RDFFormat rdfFormat) {
        asScala().export(osb::get, ReadersWriters.jenaLangWriter(rdfFormat));
    }

    public void export(File file, RDFFormat rdfFormat) {
        asScala().export(file, ReadersWriters.jenaLangWriter(rdfFormat));
    }

    public void export(String file, RDFFormat rdfFormat) {
        asScala().export(file, ReadersWriters.jenaLangWriter(rdfFormat));
    }

    public void export(File file) {
        asScala().export(file, ReadersWriters.noRdfWriter());
    }

    public void export(String file) {
        asScala().export(file, ReadersWriters.noRdfWriter());
    }

    public Index index(Index.Mode mode) {
        return Index.fromDataset(this, mode);
    }

    public Index index() {
        return Index.fromDataset(this);
    }

    public Index index(Index.Mode mode, Debugger debugger) {
        return Index.fromDataset(this, mode, debugger);
    }

    public Index index(Debugger debugger) {
        return Index.fromDataset(this, debugger);
    }

    public Ruleset mine(RulesMining rulesMining) {
        return new Ruleset(asScala().mine(rulesMining.asScala(), Debugger.empty().asScala()));
    }

    public Ruleset mine(RulesMining rulesMining, Debugger debugger) {
        return new Ruleset(asScala().mine(rulesMining.asScala(), debugger.asScala()));
    }

}