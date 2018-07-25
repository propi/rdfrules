package com.github.propi.rdfrules.java.rule;

import com.github.propi.rdfrules.rule.Measure;
import com.github.propi.rdfrules.utils.TypedKeyMap;

/**
 * Created by Vaclav Zeman on 13. 5. 2018.
 */
abstract public class RuleMeasures {

    final private TypedKeyMap.Immutable<Measure> measures;

    RuleMeasures(TypedKeyMap.Immutable<Measure> measures) {
        this.measures = measures;
    }

    public Integer getSupport() {
        return measures.get(Measure.Support$.MODULE$).fold(() -> null, Measure.Support::value);
    }

    public Double getHeadCoverage() {
        return measures.get(Measure.HeadCoverage$.MODULE$).fold(() -> null, Measure.HeadCoverage::value);
    }

    public Integer getHeadSize() {
        return measures.get(Measure.HeadSize$.MODULE$).fold(() -> null, Measure.HeadSize::value);
    }

    public Integer getBodySize() {
        return measures.get(Measure.BodySize$.MODULE$).fold(() -> null, Measure.BodySize::value);
    }

    public Double getConfidence() {
        return measures.get(Measure.Confidence$.MODULE$).fold(() -> null, Measure.Confidence::value);
    }

    public Double getHeadConfidence() {
        return measures.get(Measure.HeadConfidence$.MODULE$).fold(() -> null, Measure.HeadConfidence::value);
    }

    public Double getLift() {
        return measures.get(Measure.Lift$.MODULE$).fold(() -> null, Measure.Lift::value);
    }

    public Integer getPcaBodySize() {
        return measures.get(Measure.PcaBodySize$.MODULE$).fold(() -> null, Measure.PcaBodySize::value);
    }

    public Double getPcaConfidence() {
        return measures.get(Measure.PcaConfidence$.MODULE$).fold(() -> null, Measure.PcaConfidence::value);
    }

    public Integer getCluster() {
        return measures.get(Measure.Cluster$.MODULE$).fold(() -> null, Measure.Cluster::number);
    }

}
