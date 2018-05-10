package com.github.propi.rdfrules.java;

/**
 * Created by Vaclav Zeman on 10. 5. 2018.
 */
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

    public static TripleItemType tripleItemTypeToEnum(com.github.propi.rdfrules.data.TripleItemType tripleItemType) {
        for (TripleItemType value : TripleItemType.values()) {
            if (value.getTripleItemType().equals(tripleItemType)) {
                return value;
            }
        }
        throw new IllegalArgumentException();
    }
}
