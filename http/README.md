# RDFRules: Web Service

This is the HTTP REST web service of the RDFRules tool written in the Scala language with Akka Http. It has implemented http facades over the RDFRules core. All RDFRules operations are performed by a pipeline of tasks defined in one JSON document sent to the HTTP endpoint.

This module also supports batch processing of defined task in the JSON pipeline format.

## Getting Started

[![](https://jitpack.io/v/propi/rdfrules.svg)](https://jitpack.io/#propi/rdfrules)

Clone the RDFRules repository and run following SBT commands. It starts the HTTP web server:
```
> project http
> run
```

Or you can use it in your own SBT project:
```sbt
resolvers += "jitpack" at "https://jitpack.io"

libraryDependencies += "com.github.propi.rdfrules" %% "http" % "master"
```

You can also download .jar file with all dependencies from JitPack and simply run class ```com.github.propi.rdfrules.http.Main``` without any other parameters.

Default server parameters are:
```hocon
rdfrules {
  default-max-mining-time = 0 minutes
  server {
    host = "localhost"
    port = "8851"
    stopping-token = ""
    webapp-dir = "webapp"
  }
  cache {
    max-item-lifetime = 1 hour
  }
  workspace {
    max-uploaded-file-size = 1g
    max-files-in-directory = 100
    path = "workspace"
    writable.path = ["temp", "."]
    writable.lifetime = [1 day, Infinity]
  }
}
```
The default endpoint is listening on http://localhost:8851/api

GUI on http://localhost:8851

You can set some environment variables to change these default values:

| Environment Variable    | Parameter                      |
|-------------------------|--------------------------------|
| RDFRULES_PORT           | rdfrules.server.port           |
| RDFRULES_HOSTNAME       | rdfrules.server.host           |
| RDFRULES_STOPPING_TOKEN | rdfrules.server.stopping-token |
| RDFRULES_WORKSPACE      | rdfrules.workspace             |
| RDFRULES_WEBAPP_DIR     | rdfrules.server.webapp-dir     |

If you specify some stopping token, then the server can be stopped by HTTP GET request: ```http://<server>:<port>/api/<stopping-token>```. If the stopping token is not specified (it is default setting), then the sever is stopped by pressing the enter key in the console.

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

```
/workspace/<path-to-file>

Method: GET
Response Content-Type: application/octet-stream
Response codes:
  200: Download a file
  404: File not found
```

```
/workspace/<path-to-file>

Method: DELETE
Response Content-Type: plain/text
Response codes:
  200: Delete a file
  404: File can not be deleted
```

```
/workspace/<path-to-file>

Method: POST
Request Content-Type: multipart/form-data
Response Content-Type: text/plain
Parts:
  directory: String
  file: bytes
Response codes:
  200: Successfully uploaded
  400: NoUploadingFile, NoDirectoryField
```

### Cache

It returns info about memory. It also can clear all or individual caches saved in memory. 

```
/cache

Method: GET
Response Content-Type: application/json
Response codes:
  200: Get memory info
```

```
/cache/clear

Method: GET
Response Content-Type: text/plain
Response codes:
  200: All caches were cleared
```

```
/cache/<cache-id>

Method: DELETE
Response Content-Type: plain/text
Response codes:
  200: Clear a concrete cache by id
```

```
/cache/<cache-id>

Method: POST
Request Content-Type: application/x-www-form-urlencoded
Response Content-Type: plain/text
Fields:
  alias: String
Response codes:
  200: Put alias for a concrete cache id. If the cache id is deleted and the alias is still preserved the cached object is also preserved until all aliases are removed.
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

```
/task/<id>

Method: DELETE
Response Content-Type: text/plain
Response codes:
  202: Accepted request to interrupt the task
```

The schema for an input task in JSON:
```json
[
{
  "name": "Task name 1",
  "parameters": {}
},
{
  "name": "Task name 2",
  "parameters": {}
}
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
  "finished": "...",                        //end time - only if the task is completed
  "logs": [{"time": "...", "message": ""}], //mining logs
  "result": Array                           //some results - the result scheme depends on the final action operation
}
```

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
```
{"name": "LoadGraph", "parameters": {
  "path": "path/to/triples.ttl",                       //OPTIONAL: Path in workspace
  "url": "url/to/triples.ttl",                         //OPTIONAL: URL to a remote file
  "graphName": "..."                                   //OPTIONAL: resource URI: <...>
}}
```

RDFDataset: Loading quads into a dataset.
```
{"name": "LoadDataset", "parameters": {
  "path": "path/to/quads.nq",                        //OPTIONAL: Path in workspace
  "url": "url/to/quads.nq",                          //OPTIONAL: URL to a remote file
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

### Others operations

The simplest way to contruct a task pipeline is to use the GUI within the latest packed binary release. Individual operations are [documented](../gui/webapp/README.md) inside the GUI. It is very fast to compose operations (transformations and action) and then to export the designed pipeline into a JSON file.

Subsequently, the JSON task file can be sent either via POST method into the HTTP service to process the task (in the GUI or by your own call), or within launching the HTTP module as the first argument if you need to process the task in the batching mode. See more in the [getting started section](../README.md). 