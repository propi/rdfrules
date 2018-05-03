package com.github.propi.rdfrules.java;

import com.github.propi.rdfrules.data.Prefix;
import scala.collection.JavaConverters;

import java.util.function.Consumer;

/**
 * Created by Vaclav Zeman on 3. 5. 2018.
 */
public interface QuadsOps<SColl, JColl> {

    com.github.propi.rdfrules.data.ops.QuadsOps<SColl> asScala();

    JColl asJava(SColl scala);

    default void prefixes(Consumer<Prefix> consumer) {
        asScala().prefixes().foreach(v1 -> {
            consumer.accept(v1);
            return null;
        });
    }

    default JColl addPrefixes(Iterable<Prefix> prefixes) {
        return asJava(asScala().addPrefixes(JavaConverters.iterableAsScalaIterable(prefixes)));
    }

}
