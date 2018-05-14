package com.github.propi.rdfrules.java.data;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Supplier;

/**
 * Created by Vaclav Zeman on 3. 5. 2018.
 */
public interface Cacheable<ST, SColl, JColl>{

    com.github.propi.rdfrules.data.ops.Cacheable<ST, SColl> asScala();

    JColl asJava(SColl scala);

    default JColl cache() {
        return asJava(asScala().cache());
    }

    default JColl cache(Supplier<OutputStream> osb) {
        return asJava(asScala().cache(osb::get));
    }

    default JColl cache(Supplier<OutputStream> osb, Supplier<InputStream> isb) {
        return asJava(asScala().cache(osb::get, isb::get));
    }

}