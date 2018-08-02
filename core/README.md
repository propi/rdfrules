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
```
