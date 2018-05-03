package com.github.propi.rdfrules.java;

import java.util.Map;

/**
 * Created by Vaclav Zeman on 2. 5. 2018.
 */
public interface TriplesOps {

    com.github.propi.rdfrules.data.ops.TriplesOps asScala();

    default Map<HistogramKey, Integer> histogram(boolean subject, boolean predicate, boolean object) {
        return new MapWrapper<>(asScala().histogram(subject, predicate, object).getMap())
                .mapValues(x -> ((Integer) x))
                .mapKeys(HistogramKey::new, HistogramKey::asScala)
                .asJava();
    }

    default Iterable<PredicateInfo> types() {
        return new IterableWrapper<>(asScala().types().toIterable()).map(PredicateInfo::new).asJava();
    }

}