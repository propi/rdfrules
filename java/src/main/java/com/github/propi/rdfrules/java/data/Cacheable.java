package com.github.propi.rdfrules.java.data;

import com.github.propi.rdfrules.java.algorithm.Debugger;

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

    default JColl cache(Debugger debugger) {
        return asJava(asScala().cache(debugger.asScala()));
    }

    default JColl cache() {
        return cache(Debugger.empty());
    }

    default void cache(Supplier<OutputStream> osb, Debugger debugger) {
        asScala().cache(osb::get, debugger.asScala());
    }

    default void cache(Supplier<OutputStream> osb) {
        cache(osb, Debugger.empty());
    }

    default JColl cache(Supplier<OutputStream> osb, Supplier<InputStream> isb, Debugger debugger) {
        return asJava(asScala().cache(osb::get, isb::get, debugger.asScala()));
    }

    default JColl cache(Supplier<OutputStream> osb, Supplier<InputStream> isb) {
        return cache(osb, isb, Debugger.empty());
    }

    default JColl cache(File file) {
        return asJava(asScala().cache(file));
    }

    default JColl cache(String file) {
        return asJava(asScala().cache(file));
    }

}