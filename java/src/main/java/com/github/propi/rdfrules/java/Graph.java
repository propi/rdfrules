package com.github.propi.rdfrules.java;

import com.github.propi.rdfrules.data.RdfSource;
import com.github.propi.rdfrules.data.formats.JenaLang;
import com.github.propi.rdfrules.data.formats.JenaLang$;
import com.github.propi.rdfrules.data.formats.Tsv;
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

    Graph(com.github.propi.rdfrules.data.Graph graph) {
        this.graph = graph;
    }

    final public static TripleItem.Uri DEFAULT = new TripleItem.LongUri("");

    public static Graph fromTsv(Supplier<InputStream> isb) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(isb::get, Tsv.tsvReader()));
    }

    public static Graph fromTsv(TripleItem.Uri name, Supplier<InputStream> isb) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(name.getTripleItem(), isb::get, Tsv.tsvReader()));
    }

    public static Graph fromTsv(File file) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(file, Tsv.tsvReader()));
    }

    public static Graph fromTsv(TripleItem.Uri name, File file) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(name.getTripleItem(), file, Tsv.tsvReader()));
    }

    public static Graph fromRdfLang(Supplier<InputStream> isb, Lang lang) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(isb::get, JenaLang.jenaLangToRdfReader(new RdfSource.JenaLang(lang))));
    }

    public static Graph fromRdfLang(TripleItem.Uri name, Supplier<InputStream> isb, Lang lang) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(name.getTripleItem(), isb::get, JenaLang.jenaLangToRdfReader(new RdfSource.JenaLang(lang))));
    }

    public static Graph fromRdfLang(File file, Lang lang) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(file, JenaLang.jenaLangToRdfReader(new RdfSource.JenaLang(lang))));
    }

    public static Graph fromRdfLang(TripleItem.Uri name, File file, Lang lang) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(name.getTripleItem(), file, JenaLang.jenaLangToRdfReader(new RdfSource.JenaLang(lang))));
    }

    public static Graph fromTriples(Iterable<Triple> triples) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(ScalaConverters.toIterable(triples, Triple::asScala)));
    }

    public static Graph fromTriples(TripleItem.Uri name, Iterable<Triple> triples) {
        return new Graph(com.github.propi.rdfrules.data.Graph.apply(name.getTripleItem(), ScalaConverters.toIterable(triples, Triple::asScala)));
    }

    public static Graph fromCache(Supplier<InputStream> isb) {
        return new Graph(com.github.propi.rdfrules.data.Graph.fromCache(isb::get));
    }

    public static Graph fromCache(TripleItem.Uri name, Supplier<InputStream> isb) {
        return new Graph(com.github.propi.rdfrules.data.Graph.fromCache(name.getTripleItem(), isb::get));
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

    public void export(Supplier<OutputStream> osb, RDFFormat rdfFormat) {
        asScala().export(osb::get, JenaLang$.MODULE$.jenaFormatToRdfWriter(rdfFormat));
    }

    public Graph withName(TripleItem.Uri name) {
        return asJava(asScala().withName(name.getTripleItem()));
    }

}