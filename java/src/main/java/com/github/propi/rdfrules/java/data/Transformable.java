package com.github.propi.rdfrules.java.data;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Created by Vaclav Zeman on 2. 5. 2018.
 */
public interface Transformable<ST, JT, SColl, JColl> {

    com.github.propi.rdfrules.data.ops.Transformable<ST, SColl> asScala();

    JT asJavaItem(ST x);

    ST asScalaItem(JT x);

    JColl asJava(SColl scala);

    default JColl map(UnaryOperator<JT> f) {
        return asJava(asScala().map(x -> asScalaItem(f.apply(asJavaItem(x)))));
    }

    default JColl filter(Predicate<JT> f) {
        return asJava(asScala().filter(x -> f.test(asJavaItem(x))));
    }

    default JColl slice(int from, int until) {
        return asJava(asScala().slice(from, until));
    }

    default JColl take(int n) {
        return asJava(asScala().take(n));
    }

    default JColl drop(int n) {
        return asJava(asScala().drop(n));
    }

    default int size() {
        return asScala().size();
    }

}