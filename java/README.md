# RdfRules: Java API

This is the Java API of the RdfRules tool. It has implemented several facades over the Scala core.

## Getting Started

Maven

```xml
<repositories>
   <repository>
      <id>jitpack.io</id>
      <url>https://jitpack.io</url>
   </repository>
</repositories>

<dependency>
   <groupId>com.github.propi.rdfrules</groupId>
   <artifactId>java</artifactId>
   <version>master</version>
</dependency>
```

## Tutorial

### RDF Data Loading

Supported RDF formats:

Format | File Extension | Named Graphs |
------ | ---------------| ------------ |
Turtle | .ttl | No
N-Triples | .nt | No
N-Quads | .nq | Yes
TriG | .trig | Yes
RDF/XML | .rdf, .xml | No
JSON-LD | .jsonld, .json | Yes
TriX | .trix | Yes
TSV | .tsv | No

RdfGraph: Loading triples into one graph.
```java
import com.github.propi.rdfrules.java.data.*;

//from file - RdfRules automatically recognizes the RDF format by the file extension
Graph.fromFile("/path/to/triples.ttl");
//or
Graph.fromRdfLang("/path/to/triples.turtle", Lang.TURTLE);
//or from TSV
Graph.fromFile("/path/to/triples.tsv");
Graph.fromTsv("/path/to/triples.tsv2");
//or with graph name
Graph.fromFile("dbpedia", "/path/to/dbpedia.ttl");
//from input stream
Graph.fromFile(() -> new FileInputStream("/path/to/triples.rdf"), Lang.RDFXML);
//from iterable
Iterable<Triple> triples = ...
Graph.fromTriples("my-graph", triples);
//from cache
Graph.fromCache("my-graph", "/path/to/triples.cache");
```

RdfDataset: Loading quads into a dataset.
```java
import com.github.propi.rdfrules.java.data.*;

//one graph - create quads with the default graph name.
Dataset.fromFile("/path/to/triples.ttl");
//multiple graphs from one file
Dataset.fromFile("/path/to/triples.nq");
Dataset.fromRdfLang("/path/to/triples.nquads", Lang.NQUADS);
//multiple graphs from many files
Dataset.empty().add(Graph.fromFile("graph1", "/path/to/graph1.ttl")).add(Graph.fromFile("graph2", "/path/to/graph2.tsv"));
//or
Dataset.fromFile("/path/to/quads1.nq").add(Dataset.fromFile("/path/to/quads2.trig"));
//from cache
Dataset.fromCache("/path/to/quads.cache");
```

### RDF Data Operations

RdfGraph and RdfDataset abstractions have defined similar operations. The main difference is that RdfGraph operates with triples whereas RdfDataset operates with quads.

```java
import com.github.propi.rdfrules.java.data.*;

Graph graph = Graph.fromFile("/path/to/triples.ttl");
Dataset dataset = Dataset.fromFile("/path/to/quads.nq");
//map triples or quads
graph.map(quad -> {
       if (quad.getTriple().getPredicate().hasSameUriAs(new TripleItem.LongUri("hasChild"))) {
           return new Quad(
                   new Triple(
                           quad.getTriple().getSubject(),
                           new TripleItem.LongUri("child"),
                           quad.getTriple().getObject()
                   ),
                   quad.getGraph()
           );
       } else {
           return quad;
       }
});
dataset.map(quad -> {
    if (quad.getGraph().hasSameUriAs(new TripleItem.LongUri("dbpedia"))) {
        return new Quad(
                quad.getTriple(),
                new TripleItem.LongUri("yago")
        );
    } else {
        return quad;
    }
});
//filter triples or quads (following operations will only be shown for RdfDataset)
dataset.filter(quad -> quad.getTriple().getPredicate().hasSameUriAs(new TripleItem.LongUri("isMarriedTo")));
//take, drop or slice quads or triples
dataset.take(10);
dataset.drop(10);
dataset.slice(10, 20);
//count number of quads or triples
dataset.size();
//list all predicates, their types and amounts
Map<TripleItem.Uri, Map<TripleItemType, Integer>> types = dataset.types();
//predicates are keys of the Map, values are Maps where the key is a triple item type (Resource, Number, Boolean, Text, Interval) and values are numbers of occurences of the particular type for the specific predicate.
//make histogram for aggregated predicates
dataset.histogram(false, true, false);
//make histogram for aggregated predicates with objects
Map<HistogramKey, Integer> histogram = dataset.histogram(false, true, true);
//a histogram key consists of optional triple items: Histogram.Key(s: Option[TripleItem.Uri], p: Option[TripleItem.Uri], o: Option[TripleItem]).
//a histogram value is a number of aggregated/grouped triples by the key.
histogram.forEach((histogramKey, integer) -> System.out.println(histogramKey + ": " + integer));
//we can add prefixes to shorten long URIs and to have data more readable
.addPrefixes(Arrays.asList(
   new Prefix("dbo", "http://dbpedia.org/ontology/"),
   new Prefix("dbr", "http://dbpedia.org/resource/"))
);
//or from file in TURTLE format
//@prefix dbo: <http://dbpedia.org/ontology/> .
//@prefix dbr: <http://dbpedia.org/resource/> .
dataset.addPrefixes("/path/to/prefixes.ttl");
//then we can show all defined prefixes
dataset.prefixes(System.out::println);
//discretize all numeric literals for the "<age>" predicate into 5 bins by the equal-frequency algorithm.
dataset.discretize(new EquifrequencyDiscretizationTask(5), quad -> quad.getTriple.getPredicate.hasSameUriAs(new TripleItem.LongUri("age")));
//we can use three discretization tasks: EquidistanceDiscretizationTask, EquifrequencyDiscretizationTask and EquisizeDiscretizationTask. See below for more info.
//it is possible to discretize some parts and only return intervals
import eu.easyminer.discretization.Interval;
Interval[] intervals = dataset.discretizeAndGetIntervals(new EquifrequencyDiscretizationTask(5), quad -> quad.getTriple.getPredicate.hasSameUriAs(new TripleItem.LongUri("age")));
//cache quads or triples (with all transformations) into a binary file for later use
dataset.cache("file.cache");
//export quads or triples into a file in a selected RDF format
//there are supported only streaming formats: N-Triples, N-Quads, Turtle, TriG, TriX, TSV
dataset.export("file.ttl"); // to one graph
dataset.export("file.nq"); // to several graphs
//or into output stream
import org.apache.jena.riot.RDFFormat;
dataset.export(() -> new FileOutputStream("file.nq"), RDFFormat.NQUADS_ASCII);
dataset.export("file.tsv");
//finally we can create an Index object from RdfDataset or RdfGraph
Index index = dataset.index();
//or we can specify the index mode. There are two modes: PreservedInMemory and InUseInMemory (for more details see the root page and Index abstraction info)
import com.github.propi.rdfrules.java.index.*;
dataset.index(Index.Mode.INUSEINMEMORY);
//we can skip the Index creation and to start rule mining directly (the Index object in PreservedInMemory mode is created automatically)
dataset.mine(...);
```

Discretization tasks are only facades for implemented discretization algorithms in the [EasyMiner-Discretization](https://github.com/KIZI/EasyMiner-Discretization) library. Supported algorithms are:

Task | Parameters | Algorithm |
---- | -----------| --------- |
EquidistanceDiscretizationTask(*bins*) | *bins*: number of intervals being created | It creates intervals which have equal distance. For example for numbers \[1; 10\] and 5 bins it creates intervals 5 intervals: \[1; 2\], \[3; 4\], \[5; 6\], \[7; 8\], \[9; 10\].
EquifrequencyDiscretizationTask(*bins*, *mode*, *buffer*) | *bins*: number of intervals being created, *mode* (optional): sorting mode (EXTERNAL or INMEMORY, default is EXTERNAL), *buffer* (optional): maximal buffer limit in bytes for sorting in memory (default is 15MB) | It creates an exact number of equal-frequent intervals with various distances. The algorithm requires sorted stream of numbers. Hence, data must be sorted - sorting is performing internally with a sorting mode (INMEMORY: data are sorted in memory with buffer limit, EXTERNAL: data are sorted in memory with buffer limit or sorted on a disk if the buffer limit is exceeded).
EquisizeDiscretizationTask(*support*, *mode*, *buffer*) | *support*: a minimum support (or size) of each interval, *mode* (optional): sorting mode (EXTERNAL or INMEMORY, default is EXTERNAL), *buffer* (optional): maximal buffer limit in bytes for sorting in memory (default is 15MB) | It creates various number of equal-frequent intervals where all intervals must exceed the minimal support value. The algorithm requires sorted stream of numbers. Hence, data must be sorted - sorting is performing internally with a sorting mode (INMEMORY: data are sorted in memory with buffer limit, EXTERNAL: data are sorted in memory with buffer limit or sorted on a disk if the buffer limit is exceeded).

## Index Operations

The Index object saves data in memory into several hash tables. First, all quad items, including resources, literals, graph names and prefixes, are mapped to a unique integer. Then the program creates six fact indexes only from mapped numbers representing the whole input datasets. Data are replicated six times, therefore we should be cautious about memory. The Index object can be created in two modes:

Mode | Description |
---- | ------------|
PRESERVEDINMEMORY | Data are preserved in memory until the Index object exists. DEFAULT.
INUSEINMEMORY | Data are loaded into memory once we need them. After use we release the index.

We can operate with Index by following operations:
```java
import com.github.propi.rdfrules.java.data.*;
import com.github.propi.rdfrules.java.index.*;

//create index from dataset
Index index = Dataset.fromFile("/path/to/data.nq").index();
//create index from graph
Graph.fromFile("/path/to/data.nt").index();
//create index in different mode
Graph.fromFile("/path/to/data.nt").index(Index.Mode.INUSEINMEMORY);
//create index from cache
Index.fromCache("index.cache");
Index.fromCache(() -> new FileInputStream("index.cache"));
//get mapper which maps triple items to number or vice versa
index.tripleItemMap(mapper -> {
  System.out.println(mapper.getIndex(new TripleItem.LongUri("hasChild"))); // get a number for <hasChild> resource
  return mapper.getTripleItem(1); // get a triple item from number 1
});
//get six fact indexes
index.tripleMap(indexes -> {
  //some operations with six fact indexes
});
//evaluate all lazy vals in fact indexes such as sizes and graphs: some values are not evaluated immediately but during a mining process if we need them. This method is suitable for measurement of rule mining to eliminate all secondary calculations.
index.withEvaluatedLazyVals();
//create an RdfDataset object from Index
index.toDataset();
//serialize the Index object into a file on a disk
index.cache("data.cache");
//or with output stream
index.cache(() -> new FileOutputStream("data.cache"));
//start a rule mining process where the input parameter is a rule mining task (see below for details)
index.mine(...);
```

## Start the Rule Mining Process

We can create a rule mining task by following operations:
```java
import com.github.propi.rdfrules.java.algorithm.*;

//create the AMIE+ rule mining task with default parameters (MinHeadSize = 100, MinHeadCoverage = 0.01, MaxRuleLength = 3)
RulesMining miningTask = RulesMining.amie();
index.mine(miningTask)
//with debugger which prints progress of the mining process
Debugger.use(debugger -> {
  RulesMining miningTask = RulesMining.amie(debugger);
  index.mine(miningTask)
});
```

We can add thresholds, rule patterns and constraints to the created mining task:
```java
import com.github.propi.rdfrules.java.algorithm.*;
import com.github.propi.rdfrules.java.rule.*;

RulesMining preparedMiningTask = miningTask
  .withMinHeadCoverage(0.1)
  //add rule pattern: * => isMarriedTo(Any, Any)
  .addPattern(RulePattern.create(new RulePattern.AtomPattern().withPredicate(new TripleItem.LongUri("isMarriedTo"))))
  //add rule pattern: Any(?a, Any, <yago>) ^ Any(Any, AnyConstant) => isMarriedTo(Any, Any)
  .addPattern(RulePattern
     .create(new RulePattern.AtomPattern().withPredicate(new TripleItem.LongUri("isMarriedTo")))
     .prependBodyAtom(new RulePattern.AtomPattern().withPredicate(new RulePattern.AnyConstant()))
     .prependBodyAtom(new RulePattern.AtomPattern().withSubject('a').withGraph(new TripleItem.LongUri("yago")))
  )
  //add rule pattern: hasChild(Any, Any) => *
  .addPattern(RulePattern
     .create()
     .prependBodyAtom(new RulePattern.AtomPattern().withPredicate(new TripleItem.LongUri("hasChild")))
  )
  .withInstances(false);
index.mine(preparedMiningTask);
```

All possibilities of thresholds, patterns and constraints are described in the code and in the Java API docs -- TODO!

## RuleSet Operations

The RuleSet object is created by the mining process or can be loaded from cache.

```java
import com.github.propi.rdfrules.java.ruleset.*;

Ruleset mine = index.mine(preparedMiningTask);
//or from cache
Ruleset.fromCache(index, "rules.cache");
Ruleset.fromCache(index, () -> new FileInputStream("rules.cache"));
```

We need to attach the Index object if we load rules from cache. The RuleSet contains rules in the mapped numeric form. Hence, we need the Index object to map all numbers back to readable triple items and, of course, also to compute additional measures of significance. The RuleSet object keeps all mined rules in memory. We can transform it by filtering, mapping, sorting and computing functions.

```java
import com.github.propi.rdfrules.java.ruleset.*;

ruleset
  //filter rules
  .filter(rule -> rule.getHeadCoverage() > 0.2);
  //sort by defaults: Cluster, PcaConfidence, Lift, Confidence, HeadCoverage
  .sorted()
  //sort by selected measures
  .sortBy(RuleMeasures::getConfidence, Comparator.naturalOrder())
  //compute additional measures of significance
  .computeConfidence(0.5)
  .computePcaConfidence(0.5)
  .computeLift()
  //make clusters
  .makeClusters(/*minNeighbours =*/ 3, /*minSimilarity =*/ 0.85))
  //or you can specify our own similarity measures
  .makeClusters(
     new SimilarityCounting(SimilarityCounting.RuleSimilarityCounting.ATOMS, 0.5)
     .add(SimilarityCounting.RuleSimilarityCounting.CONFIDENCE, 0.5)
  )
  //optionally we can attach to all computing and clustering operations a debugger to print progress
  //we can cache ruleset into a file on a disk or in memory
  .cache() //into memory
  .cache("rules.cache"); //into a file
  
//we can print size of rule set
System.out.println(ruleset.size);
//or print all rules
ruleset.forEach(System.out::println);
//or export rules into a file
ruleset.export("rules.json"); //machine readable format
ruleset.export("rules.txt"); //human readable format
//or to outputstream
ruleset.exportToJson(() -> new FileOutputStream("rules.json"));
ruleset.exportToText(() -> new FileOutputStream("rules.rules"));

//we can find some rule
ResolvedRule rule = ruleset.findResolved(rule -> rule.getHeadCoverage() == 1);
//or get the head of the rule set
System.out.println(ruleset.headResolved());
//find top-k similar rules to the rule
ruleset.findSimilar(rule, 10).forEach(System.out::println);
//find top-k dissimilar rules to the rule
ruleset.findDissimilar(rule, 10).forEach(System.out::println);
```
