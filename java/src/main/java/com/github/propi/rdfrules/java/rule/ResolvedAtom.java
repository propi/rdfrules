package com.github.propi.rdfrules.java.rule;

import com.github.propi.rdfrules.java.AtomItemConverters;
import com.github.propi.rdfrules.java.ScalaResolvedAtom;
import com.github.propi.rdfrules.java.TripleItemConverters;
import com.github.propi.rdfrules.java.data.TripleItem;

import java.util.Objects;

/**
 * Created by Vaclav Zeman on 13. 5. 2018.
 */
public class ResolvedAtom {

    final private com.github.propi.rdfrules.ruleset.ResolvedRule.Atom atom;

    public ResolvedAtom(com.github.propi.rdfrules.ruleset.ResolvedRule.Atom atom) {
        this.atom = atom;
    }

    abstract public static class Item {
        abstract public ScalaResolvedAtom.ItemWrapper asScala();

        @Override
        public int hashCode() {
            return asScala().item().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Item that = (Item) obj;
            return asScala().item().equals(that.asScala().item());
        }

        @Override
        public String toString() {
            return asScala().item().toString();
        }
    }

    public static class Variable extends Item {
        final private ScalaResolvedAtom.ItemVariableWrapper variable;

        public Variable(ScalaResolvedAtom.ItemVariableWrapper variable) {
            this.variable = variable;
        }

        public Variable(char variable) {
            this(ScalaResolvedAtom.variable(variable));
        }

        public Variable(String variable) {
            this(ScalaResolvedAtom.variable(variable));
        }

        @Override
        public ScalaResolvedAtom.ItemVariableWrapper asScala() {
            return variable;
        }

        public String getValue() {
            return variable.item().value();
        }
    }

    public static class Constant extends Item {
        final private ScalaResolvedAtom.ItemConstantWrapper constant;

        public Constant(ScalaResolvedAtom.ItemConstantWrapper constant) {
            this.constant = constant;
        }

        @Override
        public ScalaResolvedAtom.ItemConstantWrapper asScala() {
            return constant;
        }

        public TripleItem getValue() {
            return TripleItemConverters.toJavaTripleItem(constant.item().tripleItem());
        }
    }

    public com.github.propi.rdfrules.ruleset.ResolvedRule.Atom asScala() {
        return atom;
    }

    public Item getSubject() {
        return AtomItemConverters.toJavaResolvedAtomItem(atom.subject());
    }

    public TripleItem.Uri getPredicate() {
        return TripleItemConverters.toJavaUri(atom.predicate());
    }

    public Item getObject() {
        return AtomItemConverters.toJavaResolvedAtomItem(atom.object());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResolvedAtom that = (ResolvedAtom) o;
        return Objects.equals(atom, that.atom);
    }

    @Override
    public int hashCode() {
        return atom.hashCode();
    }

    @Override
    public String toString() {
        return atom.toString();
    }

}