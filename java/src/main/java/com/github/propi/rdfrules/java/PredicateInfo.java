package com.github.propi.rdfrules.java;

import com.github.propi.rdfrules.data.TripleItem;

import java.util.Map;

/**
 * Created by Vaclav Zeman on 3. 5. 2018.
 */
public class PredicateInfo {

    public enum TripleItemType {
        RESOURCE(com.github.propi.rdfrules.data.TripleItemType.Resource$.MODULE$),
        NUMBER(com.github.propi.rdfrules.data.TripleItemType.Number$.MODULE$),
        BOOLEAN(com.github.propi.rdfrules.data.TripleItemType.Boolean$.MODULE$),
        TEXT(com.github.propi.rdfrules.data.TripleItemType.Text$.MODULE$),
        INTERVAL(com.github.propi.rdfrules.data.TripleItemType.Interval$.MODULE$);

        final private com.github.propi.rdfrules.data.TripleItemType tripleItemType;

        TripleItemType(com.github.propi.rdfrules.data.TripleItemType tripleItemType) {
            this.tripleItemType = tripleItemType;
        }

        public com.github.propi.rdfrules.data.TripleItemType getTripleItemType() {
            return tripleItemType;
        }
    }

    private TripleItemType tripleItemTypeToEnum(com.github.propi.rdfrules.data.TripleItemType tripleItemType) {
        for (TripleItemType value : TripleItemType.values()) {
            if (value.getTripleItemType().equals(tripleItemType)) {
                return value;
            }
        }
        throw new IllegalArgumentException();
    }

    final private com.github.propi.rdfrules.data.PredicateInfo predicateInfo;

    public PredicateInfo(com.github.propi.rdfrules.data.PredicateInfo predicateInfo) {
        this.predicateInfo = predicateInfo;
    }

    public TripleItem.Uri getUri() {
        return predicateInfo.uri();
    }

    public Map<TripleItemType, Integer> ranges() {
        return new MapWrapper<>(predicateInfo.ranges())
                .mapValues(x -> (Integer) x)
                .mapKeys(this::tripleItemTypeToEnum, TripleItemType::getTripleItemType)
                .asJava();
    }

}
