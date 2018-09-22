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


