package com.github.propi.rdfrules.java.index;

import com.github.propi.rdfrules.java.TripleItemConverters;
import com.github.propi.rdfrules.java.data.TripleItem;

/**
 * Created by Vaclav Zeman on 11. 5. 2018.
 */
public class TripleItemHashIndex {

    final private com.github.propi.rdfrules.index.TripleItemHashIndex mapper;

    public TripleItemHashIndex(com.github.propi.rdfrules.index.TripleItemHashIndex mapper) {
        this.mapper = mapper;
    }

    public int getIndex(TripleItem tripleItem) {
        return mapper.getIndex(tripleItem.asScala());
    }

    public TripleItem getTripleItem(int index) {
        return TripleItemConverters.toJavaTripleItem(mapper.getTripleItem(index));
    }

}