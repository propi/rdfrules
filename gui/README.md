# RDFRules: Graphical User Interface

The GUI for the RDFRules HTTP API is written as an HTML+JavaScript page which can be accessed from a browser. It offers simple interface to construct a mining pipelines, launch defined tasks and show a result, e.g., quads, rules, histograms, etc.

## Documentation

The GUI of RDFRules is suitable for fast pipeline construction of any mining task. If you click on the plus symbol you can add an operation to your pipeline. Operations are divided into two categories: **transformations** and **actions**. Transformation transforms results of previous operations whereas actions are final operations which launch the whole pipeline with all defined transformations. Hence, each pipeline must end with just one action operation and may contain any number of transformations.

The GUI is only facade of the [http](../http) module. For more details about individual operations see the [http](../http) folder.

Properties of operations in the GUI are described directly in the interface. Docs are fetched from [webapp README.md](./webapp/README.md)
