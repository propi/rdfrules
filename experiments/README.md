# RdfRules: Experiments

This module has implemented some experiments with the RdfRules Scala Core API, Java API and HTTP web service.

## Getting Started

Clone the RdfRules repository and run following SBT commands:
```sbt
> project experiments
> run
```

After execution of the *run* command we can choose from some experiments:
- **YagoAndDbpediaSamples**: 5 samples with DBpedia and YAGO datasets. It operates with the Scala API.
- **CompleteWorkflowScala**: one example with the complete workflow of rule mining. It operates with the Scala API.
- **CompleteWorkflowJava**: the same example as in the CompleteWorkflowScala experiments, but it operates with the Java API.
- **CompleteWorkflowHttp**: the same example as in the CompleteWorkflowScala experiments, but it operates with the HTTP web service.
