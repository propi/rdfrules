package com.github.propi.rdfrules.java.data;

import com.github.propi.rdfrules.data.Prefix$;
import com.github.propi.rdfrules.java.IterableWrapper;

import java.io.File;
import java.io.InputStream;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Created by Vaclav Zeman on 10. 5. 2018.
 */
public class Prefix {

    final private com.github.propi.rdfrules.data.Prefix prefix;

    public Prefix(com.github.propi.rdfrules.data.Prefix prefix) {
        this.prefix = prefix;
    }

    public Prefix(String prefix, String nameSpace) {
        this(Prefix$.MODULE$.apply(prefix, nameSpace));
    }

    public static Iterable<Prefix> fromFile(File file) {
        return new IterableWrapper<>(com.github.propi.rdfrules.data.Prefix.apply(file).toIterable()).map(Prefix::new).asJava();
    }

    public static Iterable<Prefix> fromInputStream(Supplier<InputStream> isb) {
        return new IterableWrapper<>(com.github.propi.rdfrules.data.Prefix.apply(isb::get).toIterable()).map(Prefix::new).asJava();
    }

    public com.github.propi.rdfrules.data.Prefix asScala() {
        return prefix;
    }

    public String getPrefix() {
        return prefix.prefix();
    }

    public String getNameSpace() {
        return prefix.nameSpace();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Prefix prefix1 = (Prefix) o;
        return Objects.equals(prefix, prefix1.prefix);
    }

    @Override
    public int hashCode() {
        return prefix.hashCode();
    }

    @Override
    public String toString() {
        return prefix.toString();
    }

}