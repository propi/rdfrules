package com.github.propi.rdfrules.java.data;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Supplier;

/**
 * Created by Vaclav Zeman on 3. 5. 2018.
 */
public interface Cacheable<ST, SColl, JColl> {

    com.github.propi.rdfrules.data.ops.Cacheable<ST, SColl> asScala();

    JColl asJava(SColl scala);

    default JColl cache() {
        return asJava(asScala().cache());
    }

    default void cache(Supplier<OutputStream> osb) {
        asScala().cache(osb::get);
    }

    default JColl cache(Supplier<OutputStream> osb, Supplier<InputStream> isb) {
        return asJava(asScala().cache(osb::get, isb::get));
    }

    default JColl cache(File file) {
        return asJava(asScala().cache(file));
    }

    default JColl cache(String file) {
        return asJava(asScala().cache(file));
    }

}