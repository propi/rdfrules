package com.github.propi.rdfrules.java.rule;

import com.github.propi.rdfrules.java.MeasuresConverters;
import com.github.propi.rdfrules.utils.TypedKeyMap;

/**
 * Created by Vaclav Zeman on 6. 8. 2018.
 */
abstract public class Measure {

    public enum Key {
        SUPPORT(MeasuresConverters.Support()),
        BODYSIZE(MeasuresConverters.BodySize()),
        CLUSTER(MeasuresConverters.Cluster()),
        CONFIDENCE(MeasuresConverters.Confidence()),
        HEADCONFIDENCE(MeasuresConverters.HeadConfidence()),
        HEADCOVERAGE(MeasuresConverters.HeadCoverage()),
        HEADSIZE(MeasuresConverters.HeadSize()),
        LIFT(MeasuresConverters.Lift()),
        PCABODYSIZE(MeasuresConverters.PcaBodySize()),
        PCACONFIDENCE(MeasuresConverters.PcaConfidence());

        final private com.github.propi.rdfrules.utils.TypedKeyMap.Key<com.github.propi.rdfrules.rule.Measure> key;

        Key(com.github.propi.rdfrules.utils.TypedKeyMap.Key<com.github.propi.rdfrules.rule.Measure> key) {
            this.key = key;
        }

        public TypedKeyMap.Key<com.github.propi.rdfrules.rule.Measure> getKey() {
            return key;
        }
    }

    abstract public com.github.propi.rdfrules.rule.Measure asScala();

    @Override
    public int hashCode() {
        return asScala().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Measure that = (Measure) obj;
        return asScala().equals(that.asScala());
    }

    @Override
    public String toString() {
        return asScala().toString();
    }

    public static class Support extends Measure {
        private com.github.propi.rdfrules.rule.Measure.Support scala;

        @Override
        public com.github.propi.rdfrules.rule.Measure.Support asScala() {
            return scala;
        }

        public Support(int value) {
            this.scala = new com.github.propi.rdfrules.rule.Measure.Support(value);
        }
    }

    public static class HeadCoverage extends Measure {
        private com.github.propi.rdfrules.rule.Measure.HeadCoverage scala;

        @Override
        public com.github.propi.rdfrules.rule.Measure.HeadCoverage asScala() {
            return scala;
        }

        public HeadCoverage(double value) {
            this.scala = new com.github.propi.rdfrules.rule.Measure.HeadCoverage(value);
        }
    }

    public static class HeadSize extends Measure {
        private com.github.propi.rdfrules.rule.Measure.HeadSize scala;

        @Override
        public com.github.propi.rdfrules.rule.Measure.HeadSize asScala() {
            return scala;
        }

        public HeadSize(int value) {
            this.scala = new com.github.propi.rdfrules.rule.Measure.HeadSize(value);
        }
    }

    public static class BodySize extends Measure {
        private com.github.propi.rdfrules.rule.Measure.BodySize scala;

        @Override
        public com.github.propi.rdfrules.rule.Measure.BodySize asScala() {
            return scala;
        }

        public BodySize(int value) {
            this.scala = new com.github.propi.rdfrules.rule.Measure.BodySize(value);
        }
    }

    public static class Confidence extends Measure {
        private com.github.propi.rdfrules.rule.Measure.Confidence scala;

        @Override
        public com.github.propi.rdfrules.rule.Measure.Confidence asScala() {
            return scala;
        }

        public Confidence(double value) {
            this.scala = new com.github.propi.rdfrules.rule.Measure.Confidence(value);
        }
    }

    public static class HeadConfidence extends Measure {
        private com.github.propi.rdfrules.rule.Measure.HeadConfidence scala;

        @Override
        public com.github.propi.rdfrules.rule.Measure.HeadConfidence asScala() {
            return scala;
        }

        public HeadConfidence(double value) {
            this.scala = new com.github.propi.rdfrules.rule.Measure.HeadConfidence(value);
        }
    }

    public static class Lift extends Measure {
        private com.github.propi.rdfrules.rule.Measure.Lift scala;

        @Override
        public com.github.propi.rdfrules.rule.Measure.Lift asScala() {
            return scala;
        }

        public Lift(double value) {
            this.scala = new com.github.propi.rdfrules.rule.Measure.Lift(value);
        }
    }

    public static class PcaBodySize extends Measure {
        private com.github.propi.rdfrules.rule.Measure.PcaBodySize scala;

        @Override
        public com.github.propi.rdfrules.rule.Measure.PcaBodySize asScala() {
            return scala;
        }

        public PcaBodySize(int value) {
            this.scala = new com.github.propi.rdfrules.rule.Measure.PcaBodySize(value);
        }
    }

    public static class PcaConfidence extends Measure {
        private com.github.propi.rdfrules.rule.Measure.PcaConfidence scala;

        @Override
        public com.github.propi.rdfrules.rule.Measure.PcaConfidence asScala() {
            return scala;
        }

        public PcaConfidence(double value) {
            this.scala = new com.github.propi.rdfrules.rule.Measure.PcaConfidence(value);
        }
    }

    public static class Cluster extends Measure {
        private com.github.propi.rdfrules.rule.Measure.Cluster scala;

        @Override
        public com.github.propi.rdfrules.rule.Measure.Cluster asScala() {
            return scala;
        }

        public Cluster(int value) {
            this.scala = new com.github.propi.rdfrules.rule.Measure.Cluster(value);
        }
    }

}