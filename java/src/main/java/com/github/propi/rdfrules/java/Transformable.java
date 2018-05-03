package com.github.propi.rdfrules.java;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Created by Vaclav Zeman on 2. 5. 2018.
 */
public interface Transformable<T, SColl, JColl> {

    com.github.propi.rdfrules.data.ops.Transformable<T, SColl> asScala();

    JColl asJava(SColl scala);

    default JColl map(UnaryOperator<T> f) {
        return asJava(asScala().map(f::apply));
    }

    default JColl filter(Predicate<T> f) {
        return asJava(asScala().filter(f::test));
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