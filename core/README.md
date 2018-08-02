# RdfRules Core - Scala API

This is the core of the RdfRules tool written in the Scala language. It has implemented main functionalities and four basic abstractions defined in the [root](https://github.com/propi/rdfrules).

## Getting Started

SBT

```sbt
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.propi.rdfrules" % "core" % "master"
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
TSV | .tsv | Not

RdfGraph: Loading triples into one graph.
```scala
import com.github.propi.rdfrules._
import org.apache.jena.riot.Lang
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
val triples: Traversable[Triple] = Traversable(Triple("s1", "p1", "o1"), Triple("s2", "p2", "o2"))
Graph("my-graph", triples)
//from cache
Graph.fromCache("my-graph", "/path/to/triples.cache")
```

RdfDataset: Loading quads into a dataset.
```scala
import com.github.propi.rdfrules._
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

RdfGraph and RdfDataset abstractions have defined similar operations. The main difference is that RdfGraph operates with triples whereas RdfDataset operates with quads.

```scala
import com.github.propi.rdfrules._
val graph = Graph("/path/to/triples.ttl")
val dataset = Graph("/path/to/quads.nq")
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
val types: Map[TripleItem.Uri, Map[TripleItemType], Int] = dataset.types()
//predicates are keys of the Map, values are Maps where the key is a triple item type (Resource, Number, Boolean, Text, Interval) and values are numbers of occurences of the particular type for the specific predicate.
types.foreach(println)
//make histogram for aggregated predicates
dataset.histogram(predicate = true)
//make histogram for aggregated predicates with objects
val histogram: Map[Histogram.Key, Int] = dataset.histogram(predicate = true, `object` = true)
//a histogram key consists of optional triple items: Histogram.Key(s: Option[TripleItem.Uri], p: Option[TripleItem.Uri], o: Option[TripleItem]).
//a histogram value is a number of aggregated/grouped triples by the key.
histogram.toList.sortBy(_._2).foreach(println)
//we can add prefixes to shorten long URIs and to have data more readable
dataset.addPrefixes(
  Prefix("dbo", "http://dbpedia.org/ontology/"),
  Prefix("dbr", "http://dbpedia.org/resource/")
)
//or from file in TURTLE format
//@prefix dbo: <http://dbpedia.org/ontology/> .
//@prefix dbr: <http://dbpedia.org/resource/> .
dataset.addPrefixes("prefixes.ttl")
//then we can show all defined prefixes
dataset.prefixes.foreach(println)
//discretize all numeric literals for the "<age>" predicate into 5 bins by the equal-frequency algorithm.
dataset.discretize(EquifrequencyDiscretizationTask(5))(quad => quad.triple.predicate.hasSameUriAs("age"))
//we can use three discretization tasks: EquidistanceDiscretizationTask, EquifrequencyDiscretizationTask and EquisizeDiscretizationTask. See below for more info.
//it is possible to discretize some parts and only return intervals
import eu.easyminer.discretization.impl.Interval
val intervals: Array[Interval] = dataset.discretizeAndGetIntervals(EquifrequencyDiscretizationTask(5))(quad => quad.triple.predicate.hasSameUriAs("age"))
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
dataset.index(Mode.InUseInMemory)
//we can skip the Index creation and to start rule mining directly (the Index object in PreservedInMemory mode is created automatically)
dataset.mine(...)
```

Discretization tasks are only facades for implemented discretization algorithms in the [EasyMiner-Discretization](https://github.com/KIZI/EasyMiner-Discretization) library. Supported algorithms are:

EquidistanceDiscretizationTask, EquifrequencyDiscretizationTask and EquisizeDiscretizationTask. 

Task | Parameters | Algorithm |
---- | -----------| --------- |
EquidistanceDiscretizationTask(*bins*) | *bins*: number of intervals being created | It creates intervals which have equal distance. For example for numbers \[1; 10\] and 5 bins it creates intervals 5 intervals: \[1; 2\], \[3; 4\], \[5; 6\], \[7; 8\], \[9; 10\].
EquifrequencyDiscretizationTask(*bins*, *mode*, *buffer*) | *bins*: number of intervals being created,<br>*mode* (optional): sorting mode (External or InMemory, default is InMemory),<br>*buffer* (optional): maximal buffer limit in bytes for sorting in memory (default is 15MB) | It creates an exact number of equal-frequent intervals with various distances. The algorithm requires sorted stream of numbers. Hence, data must be sorted - sorting is performing internally with a sorting mode (InMemory: data are sorted in memory with buffer limit, External: data are sorted in memory with buffer limit or sorted on a disk if the buffer limit is exceeded).
EquisizeDiscretizationTask(*support*, *mode*, *buffer*) | *support*: a minimum support (or size) of each interval,<br>*mode* (optional): sorting mode (External or InMemory, default is InMemory),<br>*buffer* (optional): maximal buffer limit in bytes for sorting in memory (default is 15MB) | It creates various number of equal-frequent intervals where all intervals must exceed the minimal
support value. The algorithm requires sorted stream of numbers. Hence, data must be sorted - sorting is performing internally with a sorting mode (InMemory: data are sorted in memory with buffer limit, External: data are sorted in memory with buffer limit or sorted on a disk if the buffer limit is exceeded).
