package com.github.propi.rdfrules.experiments;

import com.github.propi.rdfrules.index.TripleHashIndex;
import com.github.propi.rdfrules.java.algorithm.*;
import com.github.propi.rdfrules.java.data.*;
import com.github.propi.rdfrules.java.index.*;
import com.github.propi.rdfrules.java.rule.Rule;
import com.github.propi.rdfrules.java.rule.RuleMeasures;
import com.github.propi.rdfrules.java.rule.RulePattern;
import com.github.propi.rdfrules.java.ruleset.*;
import eu.easyminer.discretization.Interval;
import org.apache.jena.riot.Lang;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.function.*;

/**
 * Created by Vaclav Zeman on 26. 7. 2018.
 */
public class JavaComplexWorkflow {

    public static void main(String[] args) {
        Dataset.empty().mine(null).export(new RulesetWriter.Json());
        //Dataset.fromTsv(new File("")).mine(null).ex
    }

}
