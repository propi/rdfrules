package com.github.propi.rdfrules.java.data;

import com.github.propi.rdfrules.data.Prefix;
import eu.easyminer.discretization.impl.IntervalBound;

/**
 * Created by Vaclav Zeman on 10. 5. 2018.
 */
abstract public class TripleItem {

    abstract public com.github.propi.rdfrules.data.TripleItem asScala();

    @Override
    public String toString() {
        return asScala().toString();
    }

    @Override
    public int hashCode() {
        return asScala().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        TripleItem that = (TripleItem) obj;
        return asScala().equals(that.asScala());
    }

    abstract public static class Uri extends TripleItem {
        @Override
        abstract public com.github.propi.rdfrules.data.TripleItem.Uri asScala();

        public boolean hasSameUriAs(Uri uri) {
            return this.asScala().hasSameUriAs(uri.asScala());
        }
    }

    abstract public static class Literal extends TripleItem {
        @Override
        abstract public com.github.propi.rdfrules.data.TripleItem.Literal asScala();
    }

    public static class LongUri extends Uri {
        final com.github.propi.rdfrules.data.TripleItem.LongUri tripleItem;

        public LongUri(com.github.propi.rdfrules.data.TripleItem.LongUri tripleItem) {
            this.tripleItem = tripleItem;
        }

        public LongUri(String uri) {
            this(new com.github.propi.rdfrules.data.TripleItem.LongUri(uri));
        }

        @Override
        public com.github.propi.rdfrules.data.TripleItem.LongUri asScala() {
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

        public PrefixedUri(com.github.propi.rdfrules.data.TripleItem.PrefixedUri tripleItem) {
            this.tripleItem = tripleItem;
        }

        public PrefixedUri(String prefix, String nameSpace, String localName) {
            this(new com.github.propi.rdfrules.data.TripleItem.PrefixedUri(prefix, nameSpace, localName));
        }

        @Override
        public com.github.propi.rdfrules.data.TripleItem.PrefixedUri asScala() {
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

        public BlankNode(com.github.propi.rdfrules.data.TripleItem.BlankNode tripleItem) {
            this.tripleItem = tripleItem;
        }

        public BlankNode(String id) {
            this(new com.github.propi.rdfrules.data.TripleItem.BlankNode(id));
        }

        @Override
        public com.github.propi.rdfrules.data.TripleItem.BlankNode asScala() {
            return this.tripleItem;
        }

        public String getId() {
            return this.tripleItem.id();
        }
    }

    public static class Text extends Literal {
        final com.github.propi.rdfrules.data.TripleItem.Text tripleItem;

        public Text(com.github.propi.rdfrules.data.TripleItem.Text tripleItem) {
            this.tripleItem = tripleItem;
        }

        public Text(String value) {
            this(new com.github.propi.rdfrules.data.TripleItem.Text(value));
        }

        @Override
        public com.github.propi.rdfrules.data.TripleItem.Text asScala() {
            return this.tripleItem;
        }

        public String getValue() {
            return this.tripleItem.value();
        }
    }

    public static class BooleanValue extends Literal {
        final com.github.propi.rdfrules.data.TripleItem.BooleanValue tripleItem;

        public BooleanValue(com.github.propi.rdfrules.data.TripleItem.BooleanValue tripleItem) {
            this.tripleItem = tripleItem;
        }

        public BooleanValue(boolean value) {
            this(new com.github.propi.rdfrules.data.TripleItem.BooleanValue(value));
        }

        @Override
        public com.github.propi.rdfrules.data.TripleItem.BooleanValue asScala() {
            return this.tripleItem;
        }

        public boolean getValue() {
            return this.tripleItem.value();
        }
    }

    public static class Interval extends Literal {
        final com.github.propi.rdfrules.data.TripleItem.Interval tripleItem;

        public Interval(com.github.propi.rdfrules.data.TripleItem.Interval tripleItem) {
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
        public com.github.propi.rdfrules.data.TripleItem.Interval asScala() {
            return this.tripleItem;
        }

        public eu.easyminer.discretization.Interval getInterval() {
            return this.tripleItem.interval();
        }
    }

    public static class Number extends Literal {
        final com.github.propi.rdfrules.data.TripleItem.Number tripleItem;

        public Number(com.github.propi.rdfrules.data.TripleItem.Number tripleItem) {
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
        public com.github.propi.rdfrules.data.TripleItem.Number asScala() {
            return this.tripleItem;
        }

        public double getValue() {
            return tripleItem.n().toDouble(tripleItem.value());
        }
    }

}
