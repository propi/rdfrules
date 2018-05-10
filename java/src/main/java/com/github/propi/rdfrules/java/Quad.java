package com.github.propi.rdfrules.java;

import java.util.Objects;

/**
 * Created by Vaclav Zeman on 10. 5. 2018.
 */
public class Quad {

    final private com.github.propi.rdfrules.data.Quad quad;

    Quad(com.github.propi.rdfrules.data.Quad quad) {
        this.quad = quad;
    }

    public Quad(Triple triple, TripleItem.Uri graph) {
        this(new com.github.propi.rdfrules.data.Quad(triple.asScala(), graph.getTripleItem()));
    }

    public com.github.propi.rdfrules.data.Quad asScala() {
        return quad;
    }

    public Triple getTriple() {
        return new Triple(quad.triple());
    }

    public TripleItem.Uri getGraph() {
        return TripleItemConverters.toJavaUri(quad.graph());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quad quad1 = (Quad) o;
        return Objects.equals(quad, quad1.quad);
    }

    @Override
    public int hashCode() {
        return quad.hashCode();
    }

}