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
Interval[] intervals = Dataset.empty().discretizeAndGetIntervals(new EquifrequencyDiscretizationTask(5), quad -> quad.getTriple.getPredicate.hasSameUriAs(new TripleItem.LongUri("age")));
//cache quads or triples (with all transformations) into a binary file for later use
dataset.cache("file.cache");
//export quads or triples into a file in a selected RDF format
//there are supported only streaming formats: N-Triples, N-Quads, Turtle, TriG, TriX, TSV
dataset.export("file.ttl"); // to one graph
dataset.export("file.nq"); // to several graphs
//or into output stream
import org.apache.jena.riot.RDFFormat;
dataset.export(() -> new FileOutputStream("file.nq"), RDFFormat.NQUADS_ASCII);
dataset.exportToTsv(() -> new FileOutputStream("file.tsv"));
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
EquifrequencyDiscretizationTask(*bins*, *mode*, *buffer*) | *bins*: number of intervals being created, *mode* (optional): sorting mode (External or InMemory, default is InMemory), *buffer* (optional): maximal buffer limit in bytes for sorting in memory (default is 15MB) | It creates an exact number of equal-frequent intervals with various distances. The algorithm requires sorted stream of numbers. Hence, data must be sorted - sorting is performing internally with a sorting mode (InMemory: data are sorted in memory with buffer limit, External: data are sorted in memory with buffer limit or sorted on a disk if the buffer limit is exceeded).
EquisizeDiscretizationTask(*support*, *mode*, *buffer*) | *support*: a minimum support (or size) of each interval, *mode* (optional): sorting mode (External or InMemory, default is InMemory), *buffer* (optional): maximal buffer limit in bytes for sorting in memory (default is 15MB) | It creates various number of equal-frequent intervals where all intervals must exceed the minimal support value. The algorithm requires sorted stream of numbers. Hence, data must be sorted - sorting is performing internally with a sorting mode (InMemory: data are sorted in memory with buffer limit, External: data are sorted in memory with buffer limit or sorted on a disk if the buffer limit is exceeded).

## Index Operations

The Index object saves data in memory into several hash tables. First, all quad items, including resources, literals, graph names and prefixes, are mapped to a unique integer. Then the program creates six fact indexes only from mapped numbers representing the whole input datasets. Data are replicated six times, therefore we should be cautious about memory. The Index object can be created in two modes:

Mode | Description |
---- | ------------|
PreservedInMemory | Data are preserved in memory until the Index object exists. DEFAULT.
InUseInMemory | Data are loaded into memory once we need them. After use we release the index.

We can operate with Index by following operations:
```scala
import com.github.propi.rdfrules._
//create index from dataset
val index = Dataset("/path/to/data.nq").index()
//create index from graph
Graph("/path/to/data.nt").index()
//create index in different mode
Graph("/path/to/data.nt").index(Mode.InUseInMemory)
//create index from cache
Index.fromCache("index.cache")
Index.fromCache(new FileInputStream("index.cache"))
//get mapper which maps triple items to number or vice versa
index.tripleItemMap { mapper =>
  mapper.getIndex(TripleItem.Uri("hasChild")) // get a number for <hasChild> resource
  mapper.getTripleItem(1) // get a triple item from number 1
}
//get six fact indexes
index.tripleMap { indexes =>
  //print all graphs bound with predicate 1, subject 2 and object 3
  indexes.predicates(1).subjects(2).value(3).iterator.foreach(println)
  //print a number of all triples with predicate 1 and subject 2
  println(indexes.predicates(1).subjects(2).size)
}
//evaluate all lazy vals in fact indexes such as sizes and graphs: some values are not evaluated immediately but during a mining process if we need them. This method is suitable for measurement of rule mining to eliminate all secondary calculations.
index.withEvaluatedLazyVals
//create an RdfDataset object from Index
index.toDataset
//serialize the Index object into a file on a disk
index.cache("data.cache")
//or with output stream
index.cache(new FileOutputStream("data.cache"))
//start a rule mining process where the input parameter is a rule mining task (see below for details)
index.mine(...)
```

## Start the Rule Mining Process

We can create a rule mining task by following operations:
```scala
import com.github.propi.rdfrules._
//create the AMIE+ rule mining task with default parameters (MinHeadSize = 100, MinHeadCoverage = 0.01, MaxRuleLength = 3)
val miningTask = Amie()
index.mine(miningTask)
//with debugger which prints progress of the mining process
Debugger() { implicit debugger =>
  val miningTask = Amie()
  index.mine(miningTask)
}
//optionally you can attach your own logger to manage log messages
Debugger(myLogger) { implicit debugger =>
  val miningTask = Amie(myLogger)
  ...
}
```

We can add thresholds, rule patterns and constraints to the created mining task:
```scala
import com.github.propi.rdfrules._
val preparedMiningTask = miningTask
  .addThreshold(Threshold.MinHeadCoverage(0.1))
  //add rule pattern: * => isMarriedTo(Any, Any)
  .addPattern(AtomPattern(predicate = "isMarriedTo"))
  //add rule pattern: Any(?a, Any, <yago>) ^ Any(Any, AnyConstant) => isMarriedTo(Any, Any)
  .addPattern(AtomPattern(graph = "yago", subject = 'a') &: AtomPattern(`object` = AnyConstant) =>: AtomPattern(predicate = "isMarriedTo"))
  //add rule pattern: hasChild(Any, Any) => *
  .addPattern(AtomPattern(predicate = "hasChild") =>: None)
  .addConstraint(RuleConstraint.WithInstances(false))
index.mine(preparedMiningTask)
```

All possibilities of thresholds, patterns and constraints are described in the code: [Threshold.scala](https://github.com/propi/rdfrules/blob/master/core/src/main/scala/com/github/propi/rdfrules/rule/Threshold.scala), [RulePattern.scala](https://github.com/propi/rdfrules/blob/master/core/src/main/scala/com/github/propi/rdfrules/rule/RulePattern.scala), [AtomPattern.scala](https://github.com/propi/rdfrules/blob/master/core/src/main/scala/com/github/propi/rdfrules/rule/AtomPattern.scala) and [RuleConstraint.scala](https://github.com/propi/rdfrules/blob/master/core/src/main/scala/com/github/propi/rdfrules/rule/RuleConstraint.scala) (see the Scala API docs - TODO!)

## RuleSet Operations

The RuleSet object is created by the mining process or can be loaded from cache.

```scala
import com.github.propi.rdfrules._
val ruleset = index.mine(preparedMiningTask)
//or from cache
Ruleset.fromCache(index, "rules.cache")
Ruleset.fromCache(index, new FileInputStream("rules.cache"))
```

We need to attach the Index object if we load rules from cache. The RuleSet contains rules in the mapped numeric form. Hence, we need the Index object to map all numbers back to readable triple items and, of course, also to compute additional measures of significance. The RuleSet object keeps all mined rules in memory. We can transform it by filtering, mapping, sorting and computing functions.

```scala
import com.github.propi.rdfrules._
ruleset
  //map rules
  .map(rule => if (rule.head.predicate.hasSameUriAs("hasChild")) rule.copy(head = rule.head.copy(predicate = "child")) else rule)
  //filter rules
  .filter(rule => rule.measures(Measure.HeadCoverage).value > 0.2)
  //sort by defaults: Cluster, PcaConfidence, Lift, Confidence, HeadCoverage
  .sorted
  //sort by selected measures
  .sortBy(Measure.HeadCoverage, Measure.Lift)
  //sort by rule length and then by selected measures
  .sortByRuleLength(Measure.HeadCoverage)
  //compute additional measures of significance
  .computeConfidence(0.5)
  .computePcaConfidence(0.5)
  .computeLift()
  //make clusters
  .makeClusters(DbScan(minNeighbours = 3, minSimilarity = 0.85))
  //or you can specify our own similarity measures
  .makeClusters {
    implicit val ruleSimilarityCounting: SimilarityCounting[Rule.Simple] = (0.5 * AtomsSimilarityCounting) ~ (0.5 * SupportSimilarityCounting)
    DbScan()
  }
  //optionally we can attach to all computing and clustering operations a debugger to print progress
  //we can cache ruleset into a file on a disk or in memory
  .cache //into memory
  .cache("rules.cache") //into a file
  
//we can print size of rule set
println(ruleset.size)
//or print all rules
ruleset.foreach(println)
//or export rules into a file
ruleset.export("rules.json") //machine readable format
ruleset.export("rules.txt") //human readable format
//or to outputstream
ruleset.export[RulesetSource.Json.type](new FileOutputStream("rules.json"))

//we can find some rule
val rule = ruleset.find(rule => rule.measures(Measure.HeadCoverage).value == 1).get
//or get the head of the rule set
println(ruleset.head)
//find top-k similar rules to the rule
ruleset.findSimilar(rule, 10).foreach(println)
//find top-k dissimilar rules to the rule
ruleset.findDissimilar(rule, 10).foreach(println)
```
