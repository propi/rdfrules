package com.github.propi.rdfrules.java;

import java.util.Objects;

/**
 * Created by Vaclav Zeman on 10. 5. 2018.
 */
public class Triple {

    final private com.github.propi.rdfrules.data.Triple triple;

    Triple(com.github.propi.rdfrules.data.Triple triple) {
        this.triple = triple;
    }

    public Triple(TripleItem.Uri subject, TripleItem.Uri predicate, TripleItem object) {
        this(new com.github.propi.rdfrules.data.Triple(subject.getTripleItem(), predicate.getTripleItem(), object.getTripleItem()));
    }

    public com.github.propi.rdfrules.data.Triple asScala() {
        return triple;
    }

    public TripleItem.Uri getSubject() {
        return TripleItemConverters.toJavaUri(triple.subject());
    }

    public TripleItem.Uri getPredicate() {
        return TripleItemConverters.toJavaUri(triple.predicate());
    }

    public TripleItem getObject() {
        return TripleItemConverters.toJavaTripleItem(triple.object());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Triple triple1 = (Triple) o;
        return Objects.equals(triple, triple1.triple);
    }

    @Override
    public int hashCode() {
        return triple.hashCode();
    }

}