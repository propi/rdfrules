package com.github.propi.rdfrules.experiments;


import com.github.propi.rdfrules.java.algorithm.Debugger;
import com.github.propi.rdfrules.java.algorithm.RulesMining;
import com.github.propi.rdfrules.java.data.Dataset;
import com.github.propi.rdfrules.java.data.DiscretizationTask;
import com.github.propi.rdfrules.java.data.Graph;
import com.github.propi.rdfrules.java.data.TripleItem;
import com.github.propi.rdfrules.java.rule.RulePattern;
import com.github.propi.rdfrules.java.ruleset.Ruleset;

/**
 * Created by Vaclav Zeman on 26. 7. 2018.
 */
public class CompleteWorkflowJava {

    private static void printRuleset(Ruleset ruleset) {
        Example<com.github.propi.rdfrules.ruleset.Ruleset> example = new Example<com.github.propi.rdfrules.ruleset.Ruleset>() {
            @Override
            public String name() {
                return "Complete Workflow with the Java API";
            }

            @Override
            public com.github.propi.rdfrules.ruleset.Ruleset example() {
                return ruleset.asScala();
            }
        };
        example.execute(ResultPrinter.RulesetPrinter$.MODULE$);
    }

    public static void main(String[] args) {
        Debugger.use(debugger -> {
            Ruleset ruleset = Dataset.empty()
                    .add(Graph.fromFile(new TripleItem.LongUri("yago"), Example$.MODULE$.experimentsDir() + "yagoLiteralFacts.tsv.bz2"))
                    .add(Graph.fromFile(new TripleItem.LongUri("yago"), Example$.MODULE$.experimentsDir() + "yagoFacts.tsv.bz2"))
                    .filter(quad -> !quad.getTriple().getPredicate().hasSameUriAs("participatedIn"))
                    .discretize(new DiscretizationTask.Equifrequency(3), quad -> quad.getTriple().getPredicate().hasSameUriAs("hasNumberOfPeople"))
                    .mine(RulesMining.amie(debugger)
                            .withoutConstantsAtMostFunctionalVariable()
                            .withMinHeadCoverage(0.01)
                            .addPattern(RulePattern.create().prependBodyAtom(new RulePattern.AtomPattern().withPredicate(new TripleItem.LongUri("hasNumberOfPeople"))))
                            .addPattern(RulePattern.create(new RulePattern.AtomPattern().withPredicate(new TripleItem.LongUri("hasNumberOfPeople")))), debugger)
                    .computePcaConfidence(0.5, debugger)
                    .sorted()
                    .cache();
            ruleset.export(Example$.MODULE$.resultDir() + "rules-workflow-scala.json");
            ruleset.export(Example$.MODULE$.resultDir() + "rules-workflow-scala.txt");
            printRuleset(ruleset);
        });
    }

}
