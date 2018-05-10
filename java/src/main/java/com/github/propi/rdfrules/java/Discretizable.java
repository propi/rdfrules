package com.github.propi.rdfrules.java;

import eu.easyminer.discretization.DiscretizationTask;
import eu.easyminer.discretization.Interval;

import java.util.function.Predicate;

/**
 * Created by Vaclav Zeman on 3. 5. 2018.
 */
public interface Discretizable<SColl, JColl> extends QuadsOps<SColl, JColl> {

    enum Mode {
        INMEMORY(DiscretizationMode.inMemory()),
        ONDISC(DiscretizationMode.onDisc());

        final private com.github.propi.rdfrules.data.ops.Discretizable.DiscretizationMode mode;

        Mode(com.github.propi.rdfrules.data.ops.Discretizable.DiscretizationMode mode) {
            this.mode = mode;
        }

        public com.github.propi.rdfrules.data.ops.Discretizable.DiscretizationMode getMode() {
            return mode;
        }
    }

    com.github.propi.rdfrules.data.ops.Discretizable<SColl> asScala();

    default Interval[] discretizeAndGetIntervals(DiscretizationTask task, Mode mode, Predicate<Quad> f) {
        return asScala().discretizeAndGetIntervals(task, mode.getMode(), x -> f.test(new Quad(x)));
    }

    default JColl discretize(DiscretizationTask task, Mode mode, Predicate<Quad> f) {
        return asJava(asScala().discretize(task, mode.getMode(), x -> f.test(new Quad(x))));
    }

}