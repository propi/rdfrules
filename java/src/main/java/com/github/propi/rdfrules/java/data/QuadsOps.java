package com.github.propi.rdfrules.java.data;

import com.github.propi.rdfrules.java.ScalaConverters;

import java.io.File;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by Vaclav Zeman on 3. 5. 2018.
 */
public interface QuadsOps<SColl, JColl> {

    com.github.propi.rdfrules.data.ops.QuadsOps<SColl> asScala();

    JColl asJava(SColl scala);

    default void prefixes(Consumer<Prefix> consumer) {
        asScala().prefixes().foreach(v1 -> {
            consumer.accept(new Prefix(v1));
            return null;
        });
    }

    default JColl addPrefixes(Iterable<Prefix> prefixes) {
        return asJava(asScala().addPrefixes(ScalaConverters.toIterable(prefixes, Prefix::asScala)));
    }

    default JColl addPrefixes(Supplier<InputStream> isb) {
        return asJava(asScala().addPrefixes(isb::get));
    }

    default JColl addPrefixes(File file) {
        return asJava(asScala().addPrefixes(file));
    }

    default JColl addPrefixes(String file) {
        return asJava(asScala().addPrefixes(file));
    }

}