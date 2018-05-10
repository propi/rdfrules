package com.github.propi.rdfrules.java;


import com.github.propi.rdfrules.data.Triple;
import com.github.propi.rdfrules.data.TripleItem;
import scala.math.Numeric;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by Vaclav Zeman on 2. 5. 2018.
 */
public class Main {

    public static void main(String[] args) {
        /*for (Object x : JavaConverters.mapAsJavaMap(Test.histogram().getMap()).values()) {
            System.out.println(x.getClass().getName());
        }*/
        //Graph.fromTsv(new File("temp/yago.tsv")).
        //new TripleItem.Number<Integer>(5, Numeric.BigDecimalAsIfIntegral$.MODULE$)
        List<com.github.propi.rdfrules.java.TripleItem> a = new LinkedList<>();
        com.github.propi.rdfrules.java.TripleItem.Number number = new com.github.propi.rdfrules.java.TripleItem.Number(1);
        a.add(number);
        System.out.println(number.tripleItem.n());
        //System.out.println(a.get(0).toString());
    }

}
