package com.github.propi.rdfrules.java;

import com.github.propi.rdfrules.data.RdfSource;
import com.github.propi.rdfrules.data.formats.JenaLang;
import com.github.propi.rdfrules.data.formats.JenaLang$;
import com.github.propi.rdfrules.data.formats.Tsv;
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

    private Dataset(com.github.propi.rdfrules.data.Dataset dataset) {
        this.dataset = dataset;
    }

    public static Dataset empty() {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.apply());
    }

    public static Dataset fromTsv(Supplier<InputStream> isb) {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.apply(isb::get, Tsv.tsvReader()));
    }

    public static Dataset fromTsv(File file) {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.apply(file, Tsv.tsvReader()));
    }

    public static Dataset fromRdfLang(Supplier<InputStream> isb, Lang lang) {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.apply(isb::get, JenaLang.jenaLangToRdfReader(new RdfSource.JenaLang(lang))));
    }

    public static Dataset fromRdfLang(File file, Lang lang) {
        return new Dataset(com.github.propi.rdfrules.data.Dataset.apply(file, JenaLang.jenaLangToRdfReader(new RdfSource.JenaLang(lang))));
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
        asScala().export(osb::get, JenaLang$.MODULE$.jenaFormatToRdfWriter(rdfFormat));
    }

}