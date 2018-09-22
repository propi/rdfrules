# RdfRules: Graphical User Interface

The GUI for the RdfRules HTTP API is written as a HTML+JavaScript page which can be accessed from a browser. It offers simple interface to construct a mining pipeline, launch defined tasks and show a result, e.g., quads, rules, histograms, etc.

## Getting Started

### Option 1: Existing RdfRules HTTP API + GUI

If you have launched the RdfRules HTTP API, then go to the "dist/webapp" directory and specify the endpoint variable. It should be an URL to the RdfRules HTTP API. After that, open "dist/webapp/index.html" with an internet browser and make a mining pipeline.

### Option 2: Run RdfRules HTTP API + GUI

If you have not yet launched the RdfRules HTTP API, then go to the "dist/bin" directory and run the HTTP API by the "main" file (or "main.bat" for Windows). After that, open "dist/webapp/index.html" with an internet browser and make a mining pipeline.

```
> cd dist/bin
> ./main

or
> main.bat   #for Windows
```

In the "dist/bin" directory, there should be created a "workspace" directory where you can put datasets for analysis. Then, if you click on the "Load graph" or "Load dataset" operation, the content of the "workspace" directory is displayed and you can select datasets to be loaded.

### Option 3: Run RdfRules HTTP API + GUI in Docker

```
> cd dist
> docker build -t rdfrules .
> docker run --name rdfrules -p 8899:8899 -d rdfrules
```

The GUI is available on this address: http://localhost:8899/api/webapp

You can copy datasets into the "workspace" directory in the running docker container by this command:

```
> docker cp path/to/dataset rdfrules:/root/webapp/workspace
```

## Tutorial

The GUI of RdfRules is suitable for fast pipeline construction of any mining task. If you click on the plus symbol you can add an operation to your pipeline. Operations are divided into two categories: **transformations** and **actions**. Transformations transforms results of previous operations whereas actions are final operations which launch the whole pipeline with all defined transformations. Hence, each pipeline must end with just one action operation and may contain any number of transformations.

### Data operations

#### Transformations

##### Load graph

It loads an RDF graph which contains a set of triples.

Parameters:

| Name | Description | Required | Default |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| Choose a file from the workspace | You can choose one file from the workspace to be loaded as a graph. | No |  |
| URL | If you specify this parameter then the graph is loaded from the remote URL. | No |  |
| RDF format | RdfRules is able to recognize RDF format from the file extension. But if the extension is unknown you can specify the RDF format explicitly. | No | (auto) |
| Graph name | URI of the loaded graph. A value must have resource URI format in angle brackets, e.g., \<dbpedia\> | No | \<\> |
  
##### Load dataset

It loads an RDF dataset which can contain a set of quads. Dataset is considered as a set of several named graphs. It is suitable to load N-Quads, JSON-LD, TriG and TriX and preserve naming of graphs in the dataset.

Parameters:

| Name | Description | Required | Default |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| Choose a file from the workspace | You can choose one file from the workspace to be loaded as a graph. | No |  |
| URL | If you specify this parameter then the graph is loaded from the remote URL. | No |  |
| RDF format | RdfRules is able to recognize RDF format from the file extension. But if the extension is unknown you can specify the RDF format explicitly. | No | (auto) |

##### Load index

Before mining, the input datasets must be indexed into memory. It is possible to serialize this index into a file and then load back this index without any need to work with graphs and datasets. This operation only loads an index from a file and allows to skip data operations.

Parameters:

| Name | Description | Required | Default |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| Choose a file from the workspace | You can choose one file from the workspace to be loaded as an index. | Yes |  |

##### Merge datasets

If you load a graph or dataset then all following operations are related only to the last loaded graph or dataset. If you need to define operations regarding all previously loaded graphs and datasets you need to merge all to one dataset. This operation is doing just that. 

##### Map quads

Map items of quads into other items by a filter with conditions and defined replacements. You can specify a condition for matching resources, texts, numbers, booleans or intervals. Within resource and text conditions we can use a regular expression for filtering. In regular expressions we can capture groups by brackets and then refer to them in replacement by the symbol $*\<QuadItem\>\<NumberOfGroup\>*. For example: $s1 refers to group 1 in the subject, $p0 refers to whole matched text in the predicate, $o2 refers to group 2 in the object, $g1 refers to group 1 in the graph.

Condition types:

Type | Search by regexp or condition | Replacement with reference | Description |
---- | ----------------------------- | -------------------------- | ----------- |
RESOURCE | ```"<some-uri>"```, ```"prefix:(localName)"``` | ```"<some-uri-$p0>"```, ```"_:$1"``` | Resource must start and end with angle brackets or it can be a local name with a prefix. Replacement can be only the full URI or blank node (prefixed URI is not allowed).
TEXT | ```"\"some (text)\""``` | ```"\"some text $o1\""``` | Text must start and end with double quotes.
NUMBER | ```"-20.5"``` | ```"$o0 + 5"``` | Text starts with number. We can only capture the whole number and regular expression is not supported.
NUMBER | ```"> 20"```, ```"(10;20]"``` | ```"$o0 - 5"``` | For number we can use conditions: >, <, >=, <=, or intervals: (x;y), \[x;y\]
BOOLEAN | ```"true"``` | ```"false"``` | For boolean we can use only exact matching: true or false.
INTERVAL | ```"i[x;y)"``` | ```"($o1;$o2)"``` | Intervals must match the pattern. Both borders of the intervals are captured, we can refer to them in replacement.

Parameters:

| Name | Description | Required | Default |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| Search -> Subject | A condition for the subject of each quad | No |  |
| Search -> Predicate | A condition for the predicate of each quad | No |  |
| Search -> Object | A condition for the object of each quad | No |  |
| Search -> Graph | A condition for the name of the graph of each quad | No |  |
| Search -> Inverse | If the inversed is checked (true) then the conditions are inversed (negated). | No | false |
| Replacement -> Subject | A replacement for all matched subjects | No |  |
| Replacement -> Predicate | A replacement for all matched predicates | No |  |
| Replacement -> Object | A replacement for all matched objects | No |  |
| Replacement -> Graph | A replacement for all matched graphs | No |  |

##### Filter quads

Filter all quads by conditions. Conditions can be grouped and separated by the logical OR. Rules for conditions are defined in the "Map quads" operation.

Parameters:

| Name | Description | Required | Default |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| Subject | A condition for the subject of each quad | No |  |
| Predicate | A condition for the predicate of each quad | No |  |
| Object | A condition for the object of each quad | No |  |
| Graph | A condition for the name of the graph of each quad | No |  |
| Inverse | If the inversed is checked (true) then the conditions are inversed (negated). | No | false |

##### Take

Take first N quads.

Parameters:

| Name | Description | Required | Default |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| Take first N quads | A number of quads to be taken from the head | Yes |  |

##### Drop

Drop first N quads.

Parameters:

| Name | Description | Required | Default |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| Drop first N quads | A number of quads to be dropped from the head | Yes |  |

##### Slice

Slice a window from the quad set.

Parameters:

| Name | Description | Required | Default |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| From | A start index from which we will take quads | Yes |  |
| Until | A final index (exclusive) to which we will take quads | Yes |  |

##### Discretize (...)

Discretize all numeric literal values at the object position only for such quads conforming defined conditions. There are three discretization algorithms (for more details see the [http](../http) module).

Parameters:

| Name | Description | Required | Default |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| Subject | A condition for the subject of each quad | No |  |
| Predicate | A condition for the predicate of each quad | No |  |
| Object | A condition for the object of each quad | No |  |
| Graph | A condition for the name of the graph of each quad | No |  |
| Inverse | If the inversed is checked (true) then the conditions are inversed (negated). | No | false |
| Number of bins | Only for equal-distance and equal-frequency: Number of intervals to be created from all numbers. | Yes |  |
| Support |  Only for equal-size: Minimal support (relative, e.g., 0.2 = 20%) for each interval to be created. | Yes |  |

##### Cache

Cache dataset and all previous transformations into a binary file on a disk.

Parameters:

| Name | Description | Required | Default |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| Path | Path in the workspace where the cache file will be saved | Yes |  |

##### Index

Transform data into the in-memory index from which we are able to mine rules.

#### Actions

##### Cache

Cache dataset and all previous transformations into a binary file on a disk.

Parameters:

| Name | Description | Required | Default |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| Path | Path in the workspace where the cache file will be saved | Yes |  |

##### Export

Export dataset and all previous transformations into a file in an RDF format.

Parameters:

| Name | Description | Required | Default |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| Path | Path in the workspace where the file will be saved | Yes |  |
| RDF Format | RdfRules is able to recognize RDF format from the file extension. But if the extension is unknown you can specify the RDF format explicitly. | No | (auto) |

##### Get quads

Get all quads of the dataset. The result list is limited to first 10 000 quads.

##### Size

Get number of all quads.

##### Types

Get all predicates and their range types with number of objects for each type.

##### Histogram

You can check what items (subject, predicate and object) should be aggregated. For example, if you check only the object type then all quads will be aggregated by objects and it shows histogram for all these aggregated objects.

Parameters:

| Name | Description | Required | Default |
|----------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------|----------|---------|
| Subject | Aggregate values by subjects | No |  |
| Predicate | Aggregate values by predicates | No |  |
| Object | Aggregate values by objects | No |  |
