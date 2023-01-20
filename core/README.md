# RDFRules Core - Scala API

This is the core of the RDFRules tool written in the Scala language. It has implemented main functionalities and five
basic abstractions defined in the [root](https://github.com/propi/rdfrules).

## Getting Started

[![](https://jitpack.io/v/propi/rdfrules.svg)](https://jitpack.io/#propi/rdfrules)

SBT

For Scala 2.13

```sbt
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.propi.rdfrules" %% "core" % "master"
```

## Tutorial

### RDF Data Loading

Supported RDF formats:

| Format    | File Extension | Named Graphs |
|-----------|----------------|--------------|
| Turtle    | .ttl           | No           |
| N-Triples | .nt            | No           |
| N-Quads   | .nq            | Yes          |
| TriG      | .trig          | Yes          |
| RDF/XML   | .rdf, .xml     | No           |
| JSON-LD   | .jsonld, .json | Yes          |
| TriX      | .trix          | Yes          |
| TSV       | .tsv           | No           |
| SQL       | .sql           | No           |

RDFGraph: Loading triples into one graph.

```scala
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.utils.ForEach
import org.apache.jena.riot.Lang
import java.io.FileInputStream

//from file - RdfRules automatically recognizes the RDF format by the file extension
Graph("/path/to/triples.ttl")
//or
Graph("/path/to/triples.turtle")(Lang.TURTLE)
//or from TSV
Graph("/path/to/triples.tsv")
Graph("/path/to/triples.tsv2")(RdfSource.Tsv)
//or with graph name
Graph("dbpedia", "/path/to/dbpedia.ttl")
//from input stream
Graph(new FileInputStream("/path/to/triples.rdf"))(Lang.RDFXML)
//from traversable
val triples: ForEach[Triple] = ForEach(Triple("s1", "p1", "o1"), Triple("s2", "p2", "o2"))
Graph("my-graph", triples)
//from cache
Graph.fromCache("my-graph", "/path/to/triples.cache")
```

RDFDataset: Loading quads into a dataset.

```scala
import com.github.propi.rdfrules.data._
import org.apache.jena.riot.Lang
//one graph - create quads with the default graph name.
Dataset("/path/to/triples.ttl")
//multiple graphs from one file
Dataset("/path/to/triples.nq")
Dataset("/path/to/triples.nquads")(Lang.NQUADS)
//multiple graphs from many files
Dataset() + Graph("graph1", "/path/to/graph1.ttl") + Graph("graph2", "/path/to/graph2.tsv")
//or
Dataset("/path/to/quads1.nq") + Dataset("/path/to/quads2.trig")
//from cache
Dataset.fromCache("/path/to/quads.cache")
```

### RDF Data Operations

RDFGraph and RDFDataset abstractions have defined similar operations. The main difference is that RDFGraph operates with
triples whereas RDFDataset operates with quads.

```scala
import com.github.propi.rdfrules.data.{Dataset, DiscretizationTask, Graph, Histogram, Prefix, Properties, RdfSource}
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.utils.ForEach

import java.io.FileOutputStream

val graph = Graph("/path/to/triples.ttl")
val dataset = Dataset("/path/to/quads.nq")
//map triples or quads
graph.map(triple => if (triple.predicate.hasSameUriAs("hasChild")) triple.copy(predicate = "child") else triple)
dataset.map(quad => if (quad.graph.hasSameUriAs("dbpedia")) quad.copy(graph = "yago") else quad)
//filter triples or quads (following operations will only be shown for RdfDataset)
dataset.filter(quad => !quad.triple.predicate.hasSameUriAs("isMarriedTo"))
//take, drop or slice quads or triples
dataset.take(10)
dataset.drop(10)
dataset.slice(10, 20)
//count number of quads or triples
dataset.size
//list all predicates, their types and amounts 
val properties: Properties = dataset.properties()
//predicates are keys of the Map, values are Maps where the key is a triple item type (Resource, Number, Boolean, Text, Interval) and values are numbers of occurences of the particular type for the specific predicate.
properties.iterator.foreach(println)
//make histogram for aggregated predicates
dataset.histogram(predicate = true)
//make histogram for aggregated predicates with objects
val histogram: Histogram = dataset.histogram(predicate = true, `object` = true)
//a histogram key consists of optional triple items: Histogram.Key(s: Option[TripleItem.Uri], p: Option[TripleItem.Uri], o: Option[TripleItem]).
//a histogram value is a number of aggregated/grouped triples by the key.
histogram.iterator.toList.sortBy(_._2).foreach(println)
//we can add prefixes to shorten long URIs and to have data more readable
dataset.addPrefixes(ForEach(
  Prefix("dbo", "http://dbpedia.org/ontology/"),
  Prefix("dbr", "http://dbpedia.org/resource/")
))
//or from file in TURTLE format
//@prefix dbo: <http://dbpedia.org/ontology/> .
//@prefix dbr: <http://dbpedia.org/resource/> .
dataset.addPrefixes("prefixes.ttl")
//then we can show all defined prefixes
dataset.userDefinedPrefixes.foreach(println)
//discretize all numeric literals for the "<age>" predicate into 5 bins by the equal-frequency algorithm.
dataset.discretize(DiscretizationTask.Equifrequency(5))(quad => quad.triple.predicate.hasSameUriAs("age"))
//we can use three discretization tasks: Equidistance, Equifrequency and Equisize. See below for more info.
//it is possible to discretize some parts and only return intervals

import eu.easyminer.discretization.impl.Interval

val intervals: Array[Interval] = dataset.discretizeAndGetIntervals(DiscretizationTask.Equifrequency(5))(quad => quad.triple.predicate.hasSameUriAs("age"))
intervals.foreach(println)
//cache quads or triples (with all transformations) into a binary file for later use
dataset.cache("file.cache")
//export quads or triples into a file in a selected RDF format
//there are supported only streaming formats: N-Triples, N-Quads, Turtle, TriG, TriX, TSV
dataset.export("file.ttl") // to one graph
dataset.export("file.nq") // to several graphs
//or into output stream

import org.apache.jena.riot.RDFFormat

dataset.export(new FileOutputStream("file.nq"))(RDFFormat.NQUADS_ASCII)
dataset.export(new FileOutputStream("file.tsv"))(RdfSource.Tsv)
//finally we can create an Index object from RdfDataset or RdfGraph
val index: Index = dataset.index()
//or we can specify the index mode. There are two modes: PreservedInMemory and InUseInMemory (for more details see the root page and Index abstraction info)
dataset.index()
//we can skip the Index creation and to start rule mining directly (the Index object in PreservedInMemory mode is created automatically)
dataset.mine(...)
```

Discretization tasks are only facades for implemented discretization algorithms in
the [EasyMiner-Discretization](https://github.com/KIZI/EasyMiner-Discretization) library. Supported algorithms are:

| Task                                                       | Parameters                                                                                                                                                                        | Algorithm |
|------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-----------|
| DiscretizationTask.Equidistance(*bins*)                    | *                                                                                                                                                                                 |           |
| bins*: number of intervals being created                   | It creates intervals which have equal distance. For example for numbers \[1; 10\] and 5 bins it creates intervals 5 intervals: \[1; 2\], \[3; 4\], \[5; 6\], \[7; 8\], \[9; 10\]. |           |
| DiscretizationTask.Equifrequency(*bins*, *buffer*, *mode*) | *bins*: number of intervals being created, *                                                                                                                                      |           |

mode* (optional): sorting mode (External or InMemory, default is External), *
buffer* (optional): maximal buffer limit in bytes for sorting in memory (default is 15MB)               | It creates an exact number of equal-frequent intervals with various distances. The algorithm requires sorted stream of numbers. Hence, data must be sorted - sorting is performing internally with a sorting mode (InMemory: data are sorted in memory with buffer limit, External: data are sorted in memory with buffer limit or sorted on a disk if the buffer limit is exceeded).                                   |
| DiscretizationTask.Equisize(*support*, *buffer*, *mode*)   | *
support*: a minimum support (or size) of each interval, *
mode* (optional): sorting mode (External or InMemory, default is External), *
buffer* (optional): maximal buffer limit in bytes for sorting in memory (default is 15MB) | It creates various number of equal-frequent intervals where all intervals must exceed the minimal support value. The algorithm requires sorted stream of numbers. Hence, data must be sorted - sorting is performing internally with a sorting mode (InMemory: data are sorted in memory with buffer limit, External: data are sorted in memory with buffer limit or sorted on a disk if the buffer limit is exceeded). |

## Index Operations

The Index object saves data in memory into several hash tables. First, all quad items, including resources, literals,
graph names and prefixes, are mapped to a unique integer. Then the program creates several fact indexes only from mapped
numbers representing the whole input datasets. Data are replicated several times (PSO, POS, SPO, SOP, OPS), therefore we
should be cautious about memory.

We can operate with Index by following operations:

```scala
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.index._

import java.io.{FileInputStream, FileOutputStream}

//create index from dataset
val index = Dataset("/path/to/data.nq").index()
//create index from graph
Graph("/path/to/data.nt").index()
//create index from cache - partially. That means it only loads triple items index without main fact index. The fact index is loaded as soon as it is needed.
Index.fromCache("index.cache", true)
Index.fromCache(new FileInputStream("index.cache"), false)
//get mapper which maps triple items to number or vice versa
index.tripleItemMap.getIndex(TripleItem.Uri("hasChild")) // get a number for <hasChild> resource
index.tripleItemMap.getTripleItem(1) // get a triple item from number 1

//get fact indexes
//print all graphs bound with predicate 1, subject 2 and object 3
index.tripleMap.getGraphs(1, 2, 3).iterator.foreach(println)
//print a number of all triples with predicate 1 and subject 2
println(index.tripleMap.predicates(1).subjects(2).size)

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
import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.rule._
import com.github.propi.rdfrules.utils.Debugger
import com.github.propi.rdfrules.index._

//create the AMIE+ rule mining task with default parameters (MinHeadSize = 100, MinHeadCoverage = 0.01, MaxRuleLength = 3)
val miningTask = Amie()
Debugger() { implicit debugger =>
  val index = Index.fromCache("data.index", false)
  index.mine(miningTask)
}
//optionally you can attach your own logger to manage log messages
```

We can add thresholds, rule patterns and constraints to the created mining task:

```scala
import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.rule.RuleConstraint.ConstantsAtPosition.ConstantsPosition
import com.github.propi.rdfrules.rule._

val miningTask = Amie()
val index = Index.fromCache("data.index", false)
val preparedMiningTask = miningTask
  .addThreshold(Threshold.MinHeadCoverage(0.1))
  //add rule pattern: * => isMarriedTo(Any, Any)
  .addPattern(AtomPattern(predicate = TripleItem.Uri("isMarriedTo")))
  //add rule pattern: Any(?a, Any, <yago>) ^ Any(Any, AnyConstant) => isMarriedTo(Any, Any)
  .addPattern(AtomPattern(graph = TripleItem.Uri("yago"), subject = 'a') &: AtomPattern(`object` = AnyConstant) =>: AtomPattern(predicate = TripleItem.Uri("isMarriedTo")))
  //add rule pattern: hasChild(Any, Any) => *
  .addPattern(AtomPattern(predicate = TripleItem.Uri("hasChild")) =>: None)
  .addConstraint(RuleConstraint.ConstantsAtPosition(ConstantsPosition.Nowhere))
index.mine(preparedMiningTask)
```

All possibilities of thresholds, patterns and constraints are described in the
code: [Threshold.scala](https://github.com/propi/rdfrules/blob/master/core/src/main/scala/com/github/propi/rdfrules/rule/Threshold.scala)
, [RulePattern.scala](https://github.com/propi/rdfrules/blob/master/core/src/main/scala/com/github/propi/rdfrules/rule/RulePattern.scala)
, [AtomPattern.scala](https://github.com/propi/rdfrules/blob/master/core/src/main/scala/com/github/propi/rdfrules/rule/AtomPattern.scala)
and [RuleConstraint.scala](https://github.com/propi/rdfrules/blob/master/core/src/main/scala/com/github/propi/rdfrules/rule/RuleConstraint.scala)

## Ruleset Operations

The Ruleset object is created by the mining process or can be loaded from cache.

```scala
import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.ruleset._
import java.io.FileInputStream

val index = Index.fromCache("data.index", false)
val ruleset = index.mine(Amie())
//or from cache
Ruleset.fromCache(index, "rules.cache")
Ruleset.fromCache(index, new FileInputStream("rules.cache"))
```

We need to attach the Index object if we load rules from cache. The RuleSet contains rules in the mapped numeric form.
Hence, we need the Index object to map all numbers back to readable triple items and, of course, also to compute
additional measures of significance. The RuleSet object keeps all mined rules in memory. We can transform it by
filtering, mapping, sorting and computing functions.

```scala
import com.github.propi.rdfrules.algorithm.clustering.{DbScan, SimilarityCounting}
import com.github.propi.rdfrules.data._
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.rule.{Measure, Rule}
import com.github.propi.rdfrules.ruleset._
import java.io.FileOutputStream

val index = Index.fromCache("data.index", false)
val ruleset = Ruleset.fromCache(index, "rules.cache")

ruleset
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
ruleset.export(new FileOutputStream("rules.json"))(RulesetSource.Json)

//we can find some rule
val rule = ruleset.findResolved(rule => rule.measures(Measure.HeadCoverage).value == 1).get
//or get the head of the rule set
println(ruleset.headResolved)
//find top-k similar rules to the rule
ruleset.findSimilar(rule, 10).foreach(println)
//find top-k dissimilar rules to the rule
ruleset.findDissimilar(rule, 10).foreach(println)
```

## Prediction Operations

Finally, we can use mined rules to predict triples, e.g., in order to complete KG with new triples.

```scala
import com.github.propi.rdfrules.index.Index
import com.github.propi.rdfrules.ruleset.Ruleset

val index = Index.fromCache("data.index", false)
val ruleset = Ruleset.fromCache(index, "rules.cache")

val predictedTriples = ruleset.predict()
//print all predicted triples with their bound rules
predictedTriples.resolvedTriples.foreach(println)
//evaluate predicted triples: precision, recall, ...
println(predictedTriples.evaluate(true))
```