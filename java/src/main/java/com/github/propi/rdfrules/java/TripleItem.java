package com.github.propi.rdfrules.java;

import com.github.propi.rdfrules.data.Prefix;
import eu.easyminer.discretization.impl.IntervalBound;

/**
 * Created by Vaclav Zeman on 10. 5. 2018.
 */
abstract public class TripleItem {

    abstract com.github.propi.rdfrules.data.TripleItem getTripleItem();

    @Override
    public String toString() {
        return getTripleItem().toString();
    }

    @Override
    public int hashCode() {
        return getTripleItem().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TripleItem that = (TripleItem) obj;
        return getTripleItem().equals(that.getTripleItem());
    }

    abstract public static class Uri extends TripleItem {
        @Override
        abstract com.github.propi.rdfrules.data.TripleItem.Uri getTripleItem();

        public boolean hasSameUriAs(Uri uri) {
            return this.getTripleItem().hasSameUriAs(uri.getTripleItem());
        }
    }

    abstract public static class Literal extends TripleItem {
        @Override
        abstract com.github.propi.rdfrules.data.TripleItem.Literal getTripleItem();
    }

    public static class LongUri extends Uri {
        final com.github.propi.rdfrules.data.TripleItem.LongUri tripleItem;

        LongUri(com.github.propi.rdfrules.data.TripleItem.LongUri tripleItem) {
            this.tripleItem = tripleItem;
        }

        public LongUri(String uri) {
            this(new com.github.propi.rdfrules.data.TripleItem.LongUri(uri));
        }

        @Override
        com.github.propi.rdfrules.data.TripleItem.LongUri getTripleItem() {
            return this.tripleItem;
        }

        public String getUri() {
            return this.tripleItem.uri();
        }

        public PrefixedUri toPrefixedUri() {
            return new PrefixedUri(this.tripleItem.toPrefixedUri());
        }
    }

    public static class PrefixedUri extends Uri {
        final com.github.propi.rdfrules.data.TripleItem.PrefixedUri tripleItem;

        PrefixedUri(com.github.propi.rdfrules.data.TripleItem.PrefixedUri tripleItem) {
            this.tripleItem = tripleItem;
        }

        public PrefixedUri(String prefix, String nameSpace, String localName) {
            this(new com.github.propi.rdfrules.data.TripleItem.PrefixedUri(prefix, nameSpace, localName));
        }

        @Override
        com.github.propi.rdfrules.data.TripleItem.PrefixedUri getTripleItem() {
            return this.tripleItem;
        }

        public String getPrefix() {
            return this.tripleItem.prefix();
        }

        public String getNameSpace() {
            return this.tripleItem.nameSpace();
        }

        public String getLocalName() {
            return this.tripleItem.localName();
        }

        public Prefix toPrefix() {
            return this.tripleItem.toPrefix();
        }

        public LongUri toLongUri() {
            return new LongUri(this.tripleItem.toLongUri());
        }
    }

    public static class BlankNode extends Uri {
        final com.github.propi.rdfrules.data.TripleItem.BlankNode tripleItem;

        BlankNode(com.github.propi.rdfrules.data.TripleItem.BlankNode tripleItem) {
            this.tripleItem = tripleItem;
        }

        public BlankNode(String id) {
            this(new com.github.propi.rdfrules.data.TripleItem.BlankNode(id));
        }

        @Override
        com.github.propi.rdfrules.data.TripleItem.BlankNode getTripleItem() {
            return this.tripleItem;
        }

        public String getId() {
            return this.tripleItem.id();
        }
    }

    public static class Text extends Literal {
        final com.github.propi.rdfrules.data.TripleItem.Text tripleItem;

        Text(com.github.propi.rdfrules.data.TripleItem.Text tripleItem) {
            this.tripleItem = tripleItem;
        }

        public Text(String value) {
            this(new com.github.propi.rdfrules.data.TripleItem.Text(value));
        }

        @Override
        com.github.propi.rdfrules.data.TripleItem.Text getTripleItem() {
            return this.tripleItem;
        }

        public String getValue() {
            return this.tripleItem.value();
        }
    }

    public static class BooleanValue extends Literal {
        final com.github.propi.rdfrules.data.TripleItem.BooleanValue tripleItem;

        BooleanValue(com.github.propi.rdfrules.data.TripleItem.BooleanValue tripleItem) {
            this.tripleItem = tripleItem;
        }

        public BooleanValue(boolean value) {
            this(new com.github.propi.rdfrules.data.TripleItem.BooleanValue(value));
        }

        @Override
        com.github.propi.rdfrules.data.TripleItem.BooleanValue getTripleItem() {
            return this.tripleItem;
        }

        public boolean getValue() {
            return this.tripleItem.value();
        }
    }

    public static class Interval extends Literal {
        final com.github.propi.rdfrules.data.TripleItem.Interval tripleItem;

        Interval(com.github.propi.rdfrules.data.TripleItem.Interval tripleItem) {
            this.tripleItem = tripleItem;
        }

        public Interval(eu.easyminer.discretization.Interval interval) {
            this(new com.github.propi.rdfrules.data.TripleItem.Interval(
                    new eu.easyminer.discretization.impl.Interval(
                            interval.isLeftBoundClosed() ? new IntervalBound.Inclusive(interval.getLeftBoundValue()) : new IntervalBound.Exclusive(interval.getLeftBoundValue()),
                            interval.isRightBoundClosed() ? new IntervalBound.Inclusive(interval.getRightBoundValue()) : new IntervalBound.Exclusive(interval.getRightBoundValue())
                    )
            ));
        }

        @Override
        com.github.propi.rdfrules.data.TripleItem.Interval getTripleItem() {
            return this.tripleItem;
        }

        public eu.easyminer.discretization.Interval getInterval() {
            return this.tripleItem.interval();
        }
    }

    public static class Number extends Literal {
        final com.github.propi.rdfrules.data.TripleItem.Number tripleItem;

        Number(com.github.propi.rdfrules.data.TripleItem.Number tripleItem) {
            this.tripleItem = tripleItem;
        }

        public static <T> Number fromNumber(com.github.propi.rdfrules.data.TripleItem.Number<T> tripleItem) {
            return new Number(tripleItem);
        }

        public Number(java.lang.Integer value) {
            this(com.github.propi.rdfrules.java.NumberConverters.apply(value));
        }

        public Number(java.lang.Short value) {
            this(com.github.propi.rdfrules.java.NumberConverters.apply(value));
        }

        public Number(java.lang.Double value) {
            this(com.github.propi.rdfrules.java.NumberConverters.apply(value));
        }

        public Number(java.lang.Float value) {
            this(com.github.propi.rdfrules.java.NumberConverters.apply(value));
        }

        public Number(java.lang.Byte value) {
            this(com.github.propi.rdfrules.java.NumberConverters.apply(value));
        }

        public Number(java.lang.Long value) {
            this(com.github.propi.rdfrules.java.NumberConverters.apply(value));
        }

        @Override
        com.github.propi.rdfrules.data.TripleItem.Number getTripleItem() {
            return this.tripleItem;
        }

        public double getValue() {
            return tripleItem.n().toDouble(tripleItem.value());
        }
    }

}
