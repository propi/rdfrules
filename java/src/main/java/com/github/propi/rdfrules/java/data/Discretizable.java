package com.github.propi.rdfrules.java.data;

import com.github.propi.rdfrules.java.IterableWrapper;
import eu.easyminer.discretization.Interval;

import java.util.function.Predicate;

/**
 * Created by Vaclav Zeman on 3. 5. 2018.
 */
public interface Discretizable<SColl, JColl> extends QuadsOps<SColl, JColl> {

    com.github.propi.rdfrules.data.ops.Discretizable<SColl> asScala();

    default java.lang.Iterable<? extends Interval> discretizeAndGetIntervals(DiscretizationTask task, Predicate<Quad> f) {
        return new IterableWrapper<>(asScala().discretizeAndGetIntervals(task.asScala(), x -> f.test(new Quad(x)))).asJava();
    }

    default JColl discretize(DiscretizationTask task, Predicate<Quad> f) {
        return asJava(asScala().discretize(task.asScala(), x -> f.test(new Quad(x))));
    }

}