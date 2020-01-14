package com.github.propi.rdfrules.java.data;

/**
 * Created by Vaclav Zeman on 14. 1. 2020.
 */
public interface QuadsOps<SColl, JColl> {

    com.github.propi.rdfrules.data.ops.QuadsOps<SColl> asScala();

    JColl asJava(SColl scala);

}