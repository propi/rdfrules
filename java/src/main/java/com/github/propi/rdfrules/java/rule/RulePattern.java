package com.github.propi.rdfrules.java.rule;

import com.github.propi.rdfrules.java.AtomItemConverters;
import com.github.propi.rdfrules.java.data.TripleItem;
import com.github.propi.rdfrules.rule.Atom;
import scala.collection.JavaConverters;
import scala.collection.immutable.List;
import scala.collection.immutable.List$;

import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by Vaclav Zeman on 13. 5. 2018.
 */
public class RulePattern {

    final private AtomPattern consequent;
    final private List<AtomPattern> antecedent;
    final private boolean exact;

    private RulePattern(AtomPattern consequent, List<AtomPattern> antecedent, boolean exact) {
        this.consequent = consequent;
        this.antecedent = antecedent;
        this.exact = exact;
    }

    public static RulePattern create(AtomPattern consequent) {
        return new RulePattern(consequent, List$.MODULE$.empty(), false);
    }

    public static RulePattern create() {
        return create(null);
    }

    public com.github.propi.rdfrules.rule.RulePattern asScala() {
        com.github.propi.rdfrules.rule.RulePattern rulePattern;
        if (consequent == null) {
            rulePattern = com.github.propi.rdfrules.rule.RulePattern.apply();
        } else {
            rulePattern = com.github.propi.rdfrules.rule.RulePattern.apply(consequent.asScala());
        }
        for (AtomPattern atom : JavaConverters.asJavaIterable(antecedent.reverse())) {
            rulePattern = rulePattern.$amp$colon(atom.asScala());
        }
        if (exact) {
            rulePattern = rulePattern.withExact(true);
        }
        return rulePattern;
    }

    public RulePattern withExact() {
        return new RulePattern(consequent, antecedent, true);
    }

    public RulePattern prependBodyAtom(AtomPattern atomPattern) {
        return new RulePattern(consequent, antecedent.$colon$colon(atomPattern), exact);
    }

    public AtomPattern getConsequent() {
        return consequent;
    }

    public Iterator<AtomPattern> getAntecedent() {
        return JavaConverters.asJavaIterator(antecedent.iterator());
    }

    public boolean isExact() {
        return exact;
    }

    public static class AtomPattern {

        final private AtomItemPattern subject;
        final private AtomItemPattern predicate;
        final private AtomItemPattern object;
        final private AtomItemPattern graph;

        public AtomPattern(AtomItemPattern subject, AtomItemPattern predicate, AtomItemPattern object, AtomItemPattern graph) {
            this.subject = subject;
            this.predicate = predicate;
            this.object = object;
            this.graph = graph;
        }

        public AtomPattern() {
            this(Any.instance, Any.instance, Any.instance, Any.instance);
        }

        public com.github.propi.rdfrules.rule.AtomPattern asScala() {
            return new com.github.propi.rdfrules.rule.AtomPattern(
                    AtomItemConverters.toScalaAtomItemPattern(subject),
                    AtomItemConverters.toScalaAtomItemPattern(predicate),
                    AtomItemConverters.toScalaAtomItemPattern(object),
                    AtomItemConverters.toScalaAtomItemPattern(graph)
            );
        }

        public AtomPattern withSubject(AtomItemPattern subject) {
            return new AtomPattern(subject, predicate, object, graph);
        }

        public AtomPattern withSubject(TripleItem subject) {
            return new AtomPattern(new Constant(subject), predicate, object, graph);
        }

        public AtomPattern withSubject(char subject) {
            return new AtomPattern(new Variable(subject), predicate, object, graph);
        }

        public AtomPattern withPredicate(AtomItemPattern predicate) {
            return new AtomPattern(subject, predicate, object, graph);
        }

        public AtomPattern withPredicate(TripleItem predicate) {
            return new AtomPattern(subject, new Constant(predicate), object, graph);
        }

        public AtomPattern withObject(AtomItemPattern object) {
            return new AtomPattern(subject, predicate, object, graph);
        }

        public AtomPattern withObject(TripleItem object) {
            return new AtomPattern(subject, predicate, new Constant(object), graph);
        }

        public AtomPattern withObject(char object) {
            return new AtomPattern(subject, predicate, new Variable(object), graph);
        }

        public AtomPattern withGraph(AtomItemPattern graph) {
            return new AtomPattern(subject, predicate, object, graph);
        }

        public AtomPattern withGraph(TripleItem graph) {
            return new AtomPattern(subject, predicate, object, new Constant(graph));
        }

        public AtomItemPattern getSubject() {
            return subject;
        }

        public AtomItemPattern getPredicate() {
            return predicate;
        }

        public AtomItemPattern getObject() {
            return object;
        }

        public AtomItemPattern getGraph() {
            return graph;
        }
    }

    abstract public static class AtomItemPattern {
    }

    public static class Any extends AtomItemPattern {
        final public static Any instance = new Any();
    }

    public static class AnyVariable extends AtomItemPattern {
        final public static AnyVariable instance = new AnyVariable();
    }

    public static class AnyConstant extends AtomItemPattern {
        final public static AnyConstant instance = new AnyConstant();
    }

    public static class Variable extends AtomItemPattern {
        final private Atom.Variable variable;

        public Variable(int index) {
            this.variable = new Atom.Variable(index);
        }

        public Variable(char c) {
            this.variable = Atom.Item$.MODULE$.apply(c);
        }

        public int getVariable() {
            return variable.index();
        }
    }

    public static class Constant extends AtomItemPattern {
        final private TripleItem constant;

        public Constant(TripleItem constant) {
            this.constant = constant;
        }

        public TripleItem getConstant() {
            return constant;
        }
    }

    public static class OneOf extends AtomItemPattern {
        final private Iterable<AtomItemPattern> col;

        public OneOf(Iterable<AtomItemPattern> col) {
            this.col = col;
        }

        public OneOf(AtomItemPattern... items) {
            this(Arrays.asList(items));
        }

        public Iterable<AtomItemPattern> getCol() {
            return col;
        }
    }

    public static class NoneOf extends AtomItemPattern {
        final private Iterable<AtomItemPattern> col;

        public NoneOf(Iterable<AtomItemPattern> col) {
            this.col = col;
        }

        public NoneOf(AtomItemPattern... items) {
            this(Arrays.asList(items));
        }

        public Iterable<AtomItemPattern> getCol() {
            return col;
        }
    }

}