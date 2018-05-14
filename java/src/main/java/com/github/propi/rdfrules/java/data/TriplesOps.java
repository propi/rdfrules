package com.github.propi.rdfrules.java.data;

import com.github.propi.rdfrules.java.*;

import java.util.Map;

/**
 * Created by Vaclav Zeman on 2. 5. 2018.
 */
public interface TriplesOps {

    com.github.propi.rdfrules.data.ops.TriplesOps asScala();

    default Map<HistogramKey, Integer> histogram(boolean subject, boolean predicate, boolean object) {
        return new MapWrapper<>(asScala().histogram(subject, predicate, object))
                .mapValues(x -> ((Integer) x))
                .mapKeys(HistogramKey::new, HistogramKey::asScala)
                .asJava();
    }

    default Map<TripleItem.Uri, Map<TripleItemType, Integer>> types() {
        return new MapWrapper<>(asScala()
                .types()
                .mapValues(x -> new MapWrapper<>(x)
                        .mapKeys(TripleItemType::tripleItemTypeToEnum, TripleItemType::getTripleItemType)
                        .mapValues(y -> (Integer) y).asJava()
                )
        ).mapKeys(TripleItemConverters::toJavaUri, TripleItem.Uri::asScala).asJava();
    }

}