package com.github.propi.rdfrules.java.rule;

import com.github.propi.rdfrules.java.AtomItemConverters;

import java.util.Objects;

/**
 * Created by Vaclav Zeman on 13. 5. 2018.
 */
public class Atom {

    final private com.github.propi.rdfrules.rule.Atom atom;

    public Atom(com.github.propi.rdfrules.rule.Atom atom) {
        this.atom = atom;
    }

    abstract public static class Item {
        abstract public com.github.propi.rdfrules.rule.Atom.Item asScala();

        @Override
        public int hashCode() {
            return asScala().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            Item that = (Item) obj;
            return asScala().equals(that.asScala());
        }

        @Override
        public String toString() {
            return asScala().toString();
        }
    }

    public static class Variable extends Item {
        final private com.github.propi.rdfrules.rule.Atom.Variable variable;

        public Variable(com.github.propi.rdfrules.rule.Atom.Variable variable) {
            this.variable = variable;
        }

        @Override
        public com.github.propi.rdfrules.rule.Atom.Variable asScala() {
            return variable;
        }

        public int getIndex() {
            return variable.index();
        }

        public String getValue() {
            return variable.value();
        }
    }

    public static class Constant extends Item {
        final private com.github.propi.rdfrules.rule.Atom.Constant constant;

        public Constant(com.github.propi.rdfrules.rule.Atom.Constant constant) {
            this.constant = constant;
        }

        @Override
        public com.github.propi.rdfrules.rule.Atom.Constant asScala() {
            return constant;
        }

        public int getValue() {
            return constant.value();
        }
    }

    public com.github.propi.rdfrules.rule.Atom asScala() {
        return atom;
    }

    public Item getSubject() {
        return AtomItemConverters.toJavaAtomItem(atom.subject());
    }

    public int getPredicate() {
        return atom.predicate();
    }

    public Item getObject() {
        return AtomItemConverters.toJavaAtomItem(atom.object());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Atom atom1 = (Atom) o;
        return Objects.equals(atom, atom1.atom);
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