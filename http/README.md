# RdfRules: Web Service

This is the HTTP REST web service of the RdfRules tool written in the Scala language with Akka Http. It has implemented http facades over the RdfRules core. All RdfRules operations are performed by a pipeline of tasks defined in one JSON document sent to the HTTP endpoint.

## Getting Started

SBT
```sbt
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.propi.rdfrules" % "rest" % "master"
```

Start the HTTP web server from SBT:
```sbt
> run
```

You can also download .jar file with all dependencies from JitPack and simply run class ```com.github.propi.rdfrules.http.Main``` without any other parameters.

Default server parameters are:
```
rdfrules {
  server {
    host = "localhost"
    port = "8080"
    root-path = "api"
    stopping-token = ""
  }
  workspace = "workspace"
}
```
The default endpoint is on http://localhost:8080/api

You can set some environment variables to change these default values:

Environment Variable | Parameter |
---------------------| --------- |
RDFRULES_PORT | rdfrules.server.port
RDFRULES_HOSTNAME | rdfrules.server.host
RDFRULES_STOPPING_TOKEN | rdfrules.server.stopping-token
RDFRULES_WORKSPACE | rdfrules.workspace

If you specify some stopping token, then the server can be stopped by HTTP GET request: ```http://<server>:<port>/api/<stopping-token>```. If the stopping token is not specified (it is default setting), then the sever is stopped by pressing enter in the console.

You can also specify a workspace directory on the web server where input datasets are stored and from which we want to mine rules. In this directory we can also save results of mining processes or all caches. Default workspace directory is set to ```<project-directory>/workspace```.

## Tutorial

Consider the root path / as the base URL: ```http://<host>:<port>/api/```

### Workspace

Get all files and directories recursively from the workspace directory:
```
/workspace

Method: GET
Response Content-Type: application/json
```

### Task

If we have defined all tasks in a JSON document then we can send it to the server and then asynchronously ask for results.
```
/task

Method: POST
Request Content-Type: application/json
Response codes:
  400: Some syntax errors in sent task definitions
  202: The task has been accepted and is in progress
Response Headers:
  Location: URL with status and results
```

```
/task/<id>

Method: GET
Response Content-Type: application/json
Response codes:
  404: The task of this ID does not exist
  400: Some errors during processing of user inputs in sent task definitions
  500: Some unexpected errors during processing of user inputs
  202: The task is still in progress
  200: The task has been finished successfully and returned desired results.
```

The schema for an input task in JSON:
```
[
{
  "name": "Task name 1",
  "parameters": {...}
},
{
  "name": "Task name 2",
  "parameters": {...}
}
...
]
```

The schema for an error result in JSON:
```json
{
  "code": "Some text code typical for this kind of error",
  "message": "Error message"
}
```

The schema for a result or progress information in JSON:
```
{
  "id": "...",                              //task id
  "started": "...",                         //start time
  "finished": null|"...",                   //end time
  "tasks": [""|null],                       //task number is null if it is still in progress or has a string with task end time
  "logs": [{"time": "...", "message": ""}], //mining logs
  "result": null|Object|Array               //some results - the result scheme depends on the final action operation
}
```

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
```
{"name": "LoadGraph", "parameters": {
  "path": "path/to/triples.ttl",                       //OPTIONAL: Path in workspace
  "url": "url/to/triples.ttl",                         //OPTIONAL: URL to a remote file
  "format": "ttl|nt|nq|xml|json|trig|trix|tsv|cache",  //OPTIONAL: RDF format
  "graph-name": "..."                                  //OPTIONAL
}}
```

RdfDataset: Loading quads into a dataset.
```
{"name": "LoadDataset", "parameters": {
  "path": "path/to/quads.nq",                        //OPTIONAL: Path in workspace
  "url": "url/to/quads.nq",                          //OPTIONAL: URL to a remote file
  "format": "ttl|nt|nq|xml|json|trig|trix|tsv|cache" //OPTIONAL: RDF format
}}
```

In the pipeline, after loaded graph or dataset you can do transformations or actions with the last loaded dataset. Once you load another one, then following transformations or actions relate to the last loaded dataset.

```
[
{"name": "LoadGraph", "parameters": {...}},           //load graph1
{"name": "Some transformation", "parameters": {...}}, //transformation related to graph1
{"name": "Some transformation", "parameters": {...}}, //transformation related to graph1
{"name": "LoadGraph", "parameters": {...}},           //load graph2
{"name": "Some transformation", "parameters": {...}}, //transformation related to graph2
{"name": "Some transformation", "parameters": {...}}, //transformation related to graph2
{"name": "LoadDataset", "parameters": {...}},         //load dataset1
{"name": "Some transformation", "parameters": {...}}, //transformation related to dataset1
{"name": "Some transformation", "parameters": {...}}, //transformation related to dataset1
{"name": "MergeDatasets", "parameters": null},        //merge all previous datasets together
{"name": "Some transformation", "parameters": {...}}  //transformation related to graph1, graph2 and dataset1
]
```

Notice that *MergeDatasets* task merges all datasets and graphs to one and all following operations are related to all these previously defined graphs and datasets. It is considered as the final set of merged quads from all graphs.

### RDF Data Transformations

For a loaded graph or dataset you can define several transformations and actions.

```
{"name": "MapQuads", "parameters": {
  "search": {                                        //REQUIRED: search quads by regular expressions or conditions
    "subject": "regular expression or condition",    //OPTIONAL
    "predicate": "regular expression or condition",  //OPTIONAL
    "object": "regular expression or condition",     //OPTIONAL
    "graph": "regular expression or condition"       //OPTIONAL
  },
  "replacement": {                      //REQUIRED: if all defined regular expressions or conditions are valid then we replace the quad by defined replacements
    "subject": "replacement"            //OPTIONAL
    "predicate": "replacement",         //OPTIONAL
    "object": "replacement",            //OPTIONAL
    "graph": "replacement"              //OPTIONAL
  }
}}
```

In regular expressions we can capture groups by brackets and then refer to them in replacement by the symbol $*\<QuadItem\>\<NumberOfGroup\>*. For example: $s1 refers to group 1 in the subject, $p0 refers to whole matched text in the predicate, $o2 refers to group 2 in the object, $g1 refers to group 1 in the graph.

Types of triples items are distinguished as follows:

Type | Search by regexp or condition | Replacement with reference | Description |
---- | ----------------------------- | -------------------------- | ----------- |
RESOURCE | ```"<some-uri>"``` | ```"<some-uri-$p0>"``` | Resource must start and end with angle brackets.
TEXT | ```"\"some (text)\""``` | ```"\"some text $o1\""``` | Text must start and end with double quotes.
NUMBER | ```"-20.5"``` | ```"$o0 + 5"``` | Text starts with number. We can only capture the whole number and regular expression is not supported.
NUMBER | ```"> 20"```, ```"(10;20]"``` | ```"$o0 - 5"``` | For number we can use conditions: >, <, >=, <=, or intervals: (x;y), \[x;y\]
BOOLEAN | ```"true"``` | ```"false"``` | For boolean we can use only exact matching: true or false.
INTERVAL | ```"[x;y)"``` | ```"[$o1 + 5;$o2)"``` | For intervals we can use same conditions as for numbers. Both borders of the intervals are captured, we can refer to them in replacement.

For the filtering operation we can use same searching syntax as in the mapping operation.
```
{"name": "FilterQuads", "parameters": {
   "subject": "regular expression or condition",    //OPTIONAL
   "predicate": "regular expression or condition",  //OPTIONAL
   "object": "regular expression or condition",     //OPTIONAL
   "graph": "regular expression or condition"       //OPTIONAL
}}
```

Take, drop and slice operations:
```
{"name": "Take", "parameters": {
   "value": number  //REQUIRED
}},
{"name": "Drop", "parameters": {
   "value": number  //REQUIRED
}},
{"name": "Slice", "parameters": {
   "start": number  //REQUIRED
   "end": number    //REQUIRED
}}
```

Add prefixes:
```
{"name": "AddPrefixes", "parameters": {
   "path": "path/to/prefixes.ttl",              //OPTIONAL: Path in workspace (only Turtle format is supported)
   "url": "url/to/prefixes.ttl",                //OPTIONAL: URL to a remote file (only Turtle format is supported)
   "prefixes" [{                                //OPTIONAL: List of defined prefixes 
     "prefix": "shorten name",
     "namespace": "URL prefix to be shorten"
   }, ...]
}}
```

Discretization:
```
{"name": "Discretize", "parameters": {
   "subject": "regular expression or condition",    //OPTIONAL: discretize by this filter
   "predicate": "regular expression or condition",  //OPTIONAL: discretize by this filter
   "object": "regular expression or condition",     //OPTIONAL: discretize by this filter
   "graph": "regular expression or condition",      //OPTIONAL: discretize by this filter
   "task": {                                        //REQUIRED: discretization task
      "name": "name of discretization task",        //REQUIRED: EquidistanceDiscretizationTask|EquifrequencyDiscretizationTask|EquisizeDiscretizationTask
      "bins": number,                               //REQUIRED only for EquidistanceDiscretizationTask and EquifrequencyDiscretizationTask: number of intervals being created
      "mode": "INMEMORY|EXTERNAL",                  //OPTIONAL: it takes effect only for EquifrequencyDiscretizationTask and EquisizeDiscretizationTask. Default is EXTERNAL.
      "buffer": number,                             //OPTIONAL: it takes effect only for EquifrequencyDiscretizationTask and EquisizeDiscretizationTask. It is buffer limit for sorting in bytes. Default is 15000000 (15MB).        
      "support": number                             //REQUIRED only for EquisizeDiscretizationTask: each interval must exceed this minimal support value. For [0;1], it use relative support in percents. For value > 1, it use absolute support.
   }
}}
```

Task | Parameters | Algorithm |
---- | -----------| --------- |
EquidistanceDiscretizationTask | *bins*: number of intervals being created | It creates intervals which have equal distance. For example for numbers \[1; 10\] and 5 bins it creates intervals 5 intervals: \[1; 2\], \[3; 4\], \[5; 6\], \[7; 8\], \[9; 10\].
EquifrequencyDiscretizationTask | *bins*: number of intervals being created, *mode* (optional): sorting mode (EXTERNAL or INMEMORY, default is EXTERNAL), *buffer* (optional): maximal buffer limit in bytes for sorting in memory (default is 15MB) | It creates an exact number of equal-frequent intervals with various distances. The algorithm requires sorted stream of numbers. Hence, data must be sorted - sorting is performing internally with a sorting mode (INMEMORY: data are sorted in memory with buffer limit, EXTERNAL: data are sorted in memory with buffer limit or sorted on a disk if the buffer limit is exceeded).
EquisizeDiscretizationTask | *support*: a minimum support (or size) of each interval, *mode* (optional): sorting mode (EXTERNAL or INMEMORY, default is EXTERNAL), *buffer* (optional): maximal buffer limit in bytes for sorting in memory (default is 15MB) | It creates various number of equal-frequent intervals where all intervals must exceed the minimal support value (relative support: value between zero and one, absolute support = value greater than one). The algorithm requires sorted stream of numbers. Hence, data must be sorted - sorting is performing internally with a sorting mode (INMEMORY: data are sorted in memory with buffer limit, EXTERNAL: data are sorted in memory with buffer limit or sorted on a disk if the buffer limit is exceeded).

Cache dataset into memory or into a file on a disk ("Cache" task can be used also as the action which returns *null* result):
```
{"name": "CacheInMemory", "parameters": null}, // in memory caching
{"name": "Cache", "parameters": {
   "path": "path/to/data.cache"               //REQUIRED: Path in workspace for a caching file
}}
```

Transform into the Index object:
```
{"name": "Index", "parameters": null}
```

### RDF Data Actions

An action is the last task in the pipeline. It executes all transformations and returns some result.

Cache:
```
{"name": "Cache", "parameters": {
   "path": "path/to/data.cache"               //REQUIRED: Path in workspace for a caching file
}}

RESULT: null
```

Export into a file in some RDF format:
```
{"name": "ExportQuads", "parameters": {
   "path": "path/to/dataset.nq",              //REQUIRED: Path in workspace for the export
   "format": "ttl|nt|nq|trig|trix|tsv"        //OPTIONAL: If the format is not specified then the system tries to guess the format by the file extension.
}}

RESULT: null
```

Get quads (it is limited to get maximum 10 000 quads). If you need more you can slice the dataset and repeatly call this action.
```
{"name": "GetQuads", "parameters": null}

RESULT: {
  "size": number                                        //total number of quads in dataset
  "quads" [{
     "subject": "resource",
     "predicate": "resource",
     "object": "resource|text|number|boolean|interval", //resource: <...>, text: \"...\", number: 0, boolean: true|false, interval: (x,y) or [x,y]
     "graph": "resource"
  }, ...]
}
```

Size of dataset:
```
{"name": "Size", "parameters": null}

RESULT: number
```

Prefixes:
```
{"name": "Prefixes", "parameters": null}

RESULT: [{
   "prefix": "shorten name",
   "namespace": "base URI"
}, ...]
```

Predicate ranges - their types and amounts:
```
{"name": "Types", "parameters": null}

RESULT: [{
   "predicate": "resource",
   "types": [{
      "name": "RESOURCE|TEXT|NUMBER|BOOLEAN|INTERVAL",
      "amount": number
   }, ...]
}, ...]
```

Aggregate/Group triples by their items and return histogram:
Predicate ranges - their types and amounts:
```
{"name": "Types", "parameters": {
   "subject": true|false,            //OPTIONAL: group by subject, default is false,
   "predicate": true|false,          //OPTIONAL: group by predicate, default is false,
   "object": true|false              //OPTIONAL: group by object, default is false
}}

RESULT: [{
   "subject": "...",     //Subject value. Only if the subject parameter is true otherwise it is null
   "predicate": "...",   //Object value. Only if the object parameter is true otherwise it is null
   "object": "...",      //Predicate value. Only if the predicate parameter is true otherwise it is null
   "amount": number
}, ...]
```

## Index Operations

The Index object saves data in memory into several hash tables. First, all quad items, including resources, literals, graph names and prefixes, are mapped to a unique integer. Then the program creates six fact indexes only from mapped numbers representing the whole input datasets. Data are replicated six times, therefore we should be cautious about memory.

Load index from cache:
```
{"name": "LoadIndex", "parameters": {
  "path": "path/to/index.cache"                       //REQUIRED: Path in workspace
}}
```

After the Index or LoadIndex task we can define following operations.

Serialize the whole index into a file on a disk (it can be used as a transformation or action):
```
{"name": "Cache", "parameters": {
   "path": "path/to/data.cache"               //REQUIRED: Path in workspace for a caching file
}}

RETURN: null
```

Transform index back to the dataset.
```
{"name": "ToDataset", "parameters": null}
```

Finally, from the Index object we can create a rule mining task which transforms the Index object into RuleSet object:
```
{"name": "Mine", "parameters": {
   "thresholds": [{                                                        //OPTIONAL: default is MinHeadSize=100, MinHeadCoverage=0.01, MaxRuleLength=3
      "name": "MinHeadSize|MinHeadCoverage|MaxRuleLength|TopK|Timeout",    //REQUIRED
      "value": number                                                      //REQUIRED: MinHeadSize: integer greater than one, MinHeadCoverage: real number [0;1], MaxRuleLength: integer greater than one, TopK: integer greater than zero, Timeout: integer representing number of minutes
   }, ...],
   "patterns" [{
      "head": "...",
      "body": [{
         ... same format as the head ...
      }, ...]
   }, ...],
   "constraints": [{
     "name": ""
   }, ...]
}}

RESULT: null
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
