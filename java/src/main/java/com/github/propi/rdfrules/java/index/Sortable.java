package com.github.propi.rdfrules.java.index;

import com.github.propi.rdfrules.java.OrderingWrapper;
import com.github.propi.rdfrules.java.data.Transformable;

import java.util.Comparator;
import java.util.function.Function;

/**
 * Created by Vaclav Zeman on 13. 5. 2018.
 */
public interface Sortable<ST, JT, SColl, JColl> extends Transformable<ST, JT, SColl, JColl> {

    com.github.propi.rdfrules.ruleset.ops.Sortable<ST, SColl> asScala();

    default JColl sorted() {
        return asJava(asScala().sorted());
    }

    default <T> JColl sortBy(Function<JT, T> f, Comparator<T> comparator) {
        return asJava(asScala().sortBy(x -> f.apply(asJavaItem(x)), new OrderingWrapper<>(comparator)));
    }

}