package com.github.propi.rdfrules.java.data;

import com.github.propi.rdfrules.java.ReadersWriters;
import com.github.propi.rdfrules.java.ScalaConverters;
import com.github.propi.rdfrules.java.TripleItemConverters;
import com.github.propi.rdfrules.java.algorithm.RulesMining;
import com.github.propi.rdfrules.java.index.Index;
import com.github.propi.rdfrules.java.ruleset.Ruleset;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFFormat;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by Vaclav Zeman on 3. 5. 2018.
 */
public class Graph implements
        Transformable<com.github.propi.rdfrules.data.Triple, Triple, com.github.propi.rdfrules.data.Graph, Graph>,
        Cacheable<com.github.propi.rdfrules.data.Triple, com.github.propi.rdfrules.data.Graph, Graph>,
        QuadsOps<com.github.propi.rdfrules.data.Graph, Graph>,
        Discretizable<com.github.propi.rdfrules.data.Graph, Graph>,
        TriplesOps {

    final private com.github.propi.rdfrules.data.Graph graph;

    public Graph(com.github.propi.rdfrules.data.Graph graph) {
        this.graph = graph;
    }

    final public static TripleItem.Uri DEFAULT = new TripleItem.LongUri("");

    public static Graph fromTsv(Supplier<InputStream> isb) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(isb::get, ReadersWriters.tsvReader()));
    }

    public static Graph fromTsv(TripleItem.Uri name, Supplier<InputStream> isb) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(name.asScala(), isb::get, ReadersWriters.tsvReader()));
    }

    public static Graph fromTsv(File file) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(file, ReadersWriters.tsvReader()));
    }

    public static Graph fromTsv(TripleItem.Uri name, File file) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(name.asScala(), file, ReadersWriters.tsvReader()));
    }

    public static Graph fromTsv(String file) {
        return fromTsv(new File(file));
    }

    public static Graph fromTsv(TripleItem.Uri name, String file) {
        return fromTsv(name, new File(file));
    }

    public static Graph fromRdfLang(Supplier<InputStream> isb, Lang lang) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(isb::get, ReadersWriters.jenaLangReader(lang)));
    }

    public static Graph fromRdfLang(TripleItem.Uri name, Supplier<InputStream> isb, Lang lang) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(name.asScala(), isb::get, ReadersWriters.jenaLangReader(lang)));
    }

    public static Graph fromRdfLang(File file, Lang lang) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(file, ReadersWriters.jenaLangReader(lang)));
    }

    public static Graph fromRdfLang(TripleItem.Uri name, File file, Lang lang) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(name.asScala(), file, ReadersWriters.jenaLangReader(lang)));
    }

    public static Graph fromRdfLang(String file, Lang lang) {
        return fromRdfLang(new File(file), lang);
    }

    public static Graph fromRdfLang(TripleItem.Uri name, String file, Lang lang) {
        return fromRdfLang(name, new File(file), lang);
    }

    public static Graph fromFile(File file) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(file, ReadersWriters.noRdfReader()));
    }

    public static Graph fromFile(String file) {
        return fromFile(new File(file));
    }

    public static Graph fromTriples(Iterable<Triple> triples) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(ScalaConverters.toIterable(triples, Triple::asScala)));
    }

    public static Graph fromTriples(TripleItem.Uri name, Iterable<Triple> triples) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(name.asScala(), ScalaConverters.toIterable(triples, Triple::asScala)));
    }

    public static Graph fromCache(Supplier<InputStream> isb) {
        return new Graph(com.github.propi.rdfrules.data.Graph.fromCache(isb::get));
    }

    public static Graph fromCache(TripleItem.Uri name, Supplier<InputStream> isb) {
        return new Graph(com.github.propi.rdfrules.data.Graph.fromCache(name.asScala(), isb::get));
    }

    public static Graph fromCache(File file) {
        return new Graph(com.github.propi.rdfrules.data.Graph.fromCache(file));
    }

    public static Graph fromCache(TripleItem.Uri name, File file) {
        return new Graph(com.github.propi.rdfrules.data.Graph.fromCache(name.asScala(), file));
    }

    public static Graph fromCache(String file) {
        return fromCache(new File(file));
    }

    public static Graph fromCache(TripleItem.Uri name, String file) {
        return fromCache(name, new File(file));
    }

    @Override
    public com.github.propi.rdfrules.data.Graph asScala() {
        return graph;
    }

    @Override
    public Graph asJava(com.github.propi.rdfrules.data.Graph scala) {
        return new Graph(scala);
    }

    @Override
    public Triple asJavaItem(com.github.propi.rdfrules.data.Triple x) {
        return new Triple(x);
    }

    @Override
    public com.github.propi.rdfrules.data.Triple asScalaItem(Triple x) {
        return x.asScala();
    }

    public TripleItem.Uri getName() {
        return TripleItemConverters.toJavaUri(asScala().name());
    }

    public void forEach(Consumer<Triple> consumer) {
        asScala().triples().foreach(v1 -> {
            consumer.accept(asJavaItem(v1));
            return null;
        });
    }

    public void export(Supplier<OutputStream> osb) {
        asScala().export(osb::get, ReadersWriters.tsvWriter());
    }

    public void export(Supplier<OutputStream> osb, RDFFormat rdfFormat) {
        asScala().export(osb::get, ReadersWriters.jenaLangWriter(rdfFormat));
    }

    public void export(File file) {
        asScala().export(file, ReadersWriters.noRdfWriter());
    }

    public void export(File file, RDFFormat rdfFormat) {
        asScala().export(file, ReadersWriters.jenaLangWriter(rdfFormat));
    }

    public void export(String file) {
        asScala().export(file, ReadersWriters.noRdfWriter());
    }

    public void export(String file, RDFFormat rdfFormat) {
        asScala().export(file, ReadersWriters.jenaLangWriter(rdfFormat));
    }

    public Graph withName(TripleItem.Uri name) {
        return asJava(asScala().withName(name.asScala()));
    }

    public Dataset toDataset() {
        return Dataset.fromGraph(this);
    }

    public Index index(Index.Mode mode) {
        return toDataset().index(mode);
    }

    public Index index() {
        return toDataset().index();
    }

    public Ruleset mine(RulesMining rulesMining) {
        return toDataset().mine(rulesMining);
    }

}