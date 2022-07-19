# RDFRules: GUI operations documentation

*popis* rdf rules [bobo](https://duckduckgo.com)

## Dataset

### Loading

#### Load graph

Load graph (set of triples) from a file in the workspace or from a remote file available via URL. The source is in some RDF format or serialized format and is supposed as a single graph.

##### Properties

- **Choose a file from the workspace**: It is possible to load a file from the workspace on the server side (just click onto a file name), or you can load any remote file from URL (see below).
- **URL**: A URL to a remote file to be loaded. If this is specified then the workspace file is ommited.
- **RDF format**: The RDF format is automatically detected from the file extension. But, you can specify the format explicitly.
- **Graph name**: Name for this loaded graph. It must have the URI notation in angle brackets, e.g., <dbpedia> or `<http://dbpedia.org>`.

#### Load dataset

Load dataset (set of quads) from a file in the workspace or from a remote file available via URL. The source is in some RDF format or serialized format and can involve several graphs.

##### Properties

- **Choose a file from the workspace**: It is possible to load a file from the workspace on the server side (just click onto a file name), or you can load any remote file from URL (see below).
- **URL**: A URL to a remote file to be loaded. If this is specified then the workspace file is ommited.
- **RDF format**: The RDF format is automatically detected from the file extension. But, you can specify the format explicitly.

### Transformations

#### Add prefixes

Add prefixes to datasets to shorten URIs.

##### Properties

- **Choose a file from the workspace**: It is possible to load a file with prefixes in the Turtle (.ttl) format from the workspace on the server side (just click onto a file name), or you can load any remote prefix file from URL (see below).
- **URL**: A URL to a remote file with prefixes in the Turtle (.ttl) format to be loaded. If this is specified then the workspace file is ommited.
- **Hand-defined prefixes**: Here, you can define your own prefixes manually.
  - **Prefix**: A short name for the namespace.
  - **Namespace**: A namespace URI to be shortened. It should end with the slash or hash symbol, e.g., `http://dbpedia.org/property/`.

#### Merge datasets

Merge all previously loaded graphs and datasets to one dataset.

#### Map quads

Map/Replace selected quads and their parts by user-defined filters and replacements.

##### Properties

- **Search**: Search quads by this filter and then replace found quads by defined replacements. Some filters can capture parts of quads and their contents.
  - **Subject**: Filter for the subject position. If this field is empty then no filter is applied here. The subject must be written in URI format in angle brackets, e.g, `<http://dbpedia.org/resource/Rule>`, or as a prefixed URI, e.g., dbr:Rule. The content is evaluated as a regular expression.
  - **Predicate**: Filter for the predicate position. If this field is empty then no filter is applied here. The predicate must be written in URI format in angle brackets, e.g, `<https://www.w3.org/2000/01/rdf-schema#label>`, or as a prefixed URI, e.g., rdfs:label. The content is evaluated as a regular expression.
  - **Object**: Filter for the object position. If this field is empty then no filter is applied here. The content is evaluated as a regular expression. You can filter resources (with regexp) and literals (with regexp and conditions). Literals can be text, number, boolean or interval. For TEXT, the content must be in double quotation marks. For NUMBER, you can use exact matching or conditions, e.g., '> 10' or intervals [10;80). For BOOLEAN, there are valid only two values true|false. For INTERVAL, you can use only exact matching like this: i[10;50); it must start with 'i' character.
  - **Graph**: Filter for the graph position. If this field is empty then no filter is applied here. The graph must be written in URI format in angle brackets, e.g, `<http://dbpedia.org>`. The content is evaluated as a regular expression.
  - **Negation**: If this field is checked then all defined filters (above) are negated (logical NOT is applied before all filters).
- **Replacement**: Replace found quads and their parts with replacements. Here, you can also refer to captured parts in regular expressions.
  - **Subject**: Replacement can be only the full URI or blank node (prefixed URI is not allowed). If this field is empty then no replace is applied here. You can refer to captured parts and groups of found quad, e.g, $0 = the full match, $1 = captured group 1 in regexp, $p1 = captured group in regexp in the predicate position.
  - **Predicate**: Replacement can be only the full URI or blank node (prefixed URI is not allowed). If this field is empty then no replace is applied here. You can refer to captured parts and groups of found quad, e.g, $0 = the full match, $1 = captured group 1 in regexp, $s1 = captured group in regexp in the subject position.
  - **Object**: For RESOURCE, the replacement can be only the full URI or blank node (prefixed URI is not allowed). For TEXT, the replacement must start and end with double quotes. For NUMBER, the replacement must be a number or some arithmetic evaluation with captured value, e.g., $o0 + 5 (it adds 5 to original numeric value). For BOOLEAN, there are only two valid values true|false. For INTERVAL, the replacement has the interval form, e.g, (10;80] (both borders of the found interval are captured, we can refer to them in replacement: [$o1;$o2]). If this field is empty then no replace is applied here. You can refer to captured parts and groups of found quad, e.g, $0 = the full match, $1 = captured group 1 in regexp, $s1 = captured group in regexp in the subject position.
  - **Graph**: Replacement can be only the full URI or blank node (prefixed URI is not allowed). If this field is empty then no replace is applied here. You can refer to captured parts and groups of found quad, e.g, $0 = the full match, $1 = captured group 1 in regexp, $s1 = captured group in regexp in the subject position.
#### Shrink

Slice the dataset (set of quads) with a specified window.

#### Discretize

Discretize all numeric literals related to filtered quads by a selected discretization strategy.

##### Properties

- **Subject**: Discretize all numeric literals which are related to this specifed subject. If this field is empty then no filter is applied here. The subject must be written in URI format in angle brackets, e.g, `<http://dbpedia.org/resource/Rule>`, or as a prefixed URI, e.g., dbr:Rule.
- **Predicate**: Discretize all numeric literals which are related to this specifed predicate. If this field is empty then no filter is applied here. The predicate must be written in URI format in angle brackets, e.g, `<https://www.w3.org/2000/01/rdf-schema#label>`, or as a prefixed URI, e.g., rdfs:label.
- **Object**: Discretize all numeric literals which are matching this object. If this field is empty then no filter is applied here. The object must be a numeric comparison, e.g, '> 10' or '(10;80]'.
- **Graph**: Discretize all numeric literals which are related to this specifed graph. If this field is empty then no filter is applied here. The graph must be written in URI format in angle brackets, e.g, `<http://dbpedia.org>`.
- **Negation**: If this field is checked then all defined filters (above) are negated (logical NOT is applied before all filters).
- **Strategy**:
  - **Name**:
  - **Equidistance or Equifrequency**:
    - **Number of bins**: Number of intervals to be created.
  - **Equisize**:
    - **Min support**: The minimal relative support which must reach each interval. The valid range is between 0 and 1. 

#### Cache

Serialize loaded dataset into a file in the workspace at the server side for later use.

##### Properties

- **In-memory**: Choose whether to save all previous transformations into memory or disk.
  - **Cache ID**: The cache identifier in the memory.
- **On-disk**:
  - **Path**: A relative path to a file related to the workspace where the serialized dataset should be saved.
- **Revalidate**: Check this if you want to re-create the cache from the previous transformations.

#### Filter quads

Filter all quads by user-defined conditions.

##### Properties

- **Filter by (logical OR)**: Defined quad filters. It filters such quads which satisfy defined conditions.
  - **Subject**: Filter for the subject position. If this field is empty then no filter is applied here. The subject must be written in URI format in angle brackets, e.g, `<http://dbpedia.org/resource/Rule>`, or as a prefixed URI, e.g., dbr:Rule. The content is evaluated as a regular expression.
  - **Predicate**: Filter for the predicate position. If this field is empty then no filter is applied here. The predicate must be written in URI format in angle brackets, e.g, `<https://www.w3.org/2000/01/rdf-schema#label>`, or as a prefixed URI, e.g., rdfs:label. The content is evaluated as a regular expression.
  - **Object**: Filter for the object position. If this field is empty then no filter is applied here. The content is evaluated as a regular expression. You can filter resources (with regexp) and literals (with regexp and conditions). Literals can be text, number, boolean or interval. For TEXT, the content must be in double quotation marks. For NUMBER, you can use exact matching or conditions, e.g., '> 10' or intervals [10;80). For BOOLEAN, there are valid only two values true|false. For INTERVAL, you can use only exact matching like this: i[10;50); it must start with 'i' character.
  - **Graph**: Filter for the graph position. If this field is empty then no filter is applied here. The graph must be written in URI format in angle brackets, e.g, `<http://dbpedia.org>`. The content is evaluated as a regular expression.
  - **Negation**: If this field is checked then all defined filters (above) are negated (logical NOT is applied before all filters).

#### Load graph

Load graph (set of triples) from a file in the workspace or from a remote file available via URL. The source is in some RDF format or serialized format and is supposed as a single graph.

##### Properties

- **Choose a file from the workspace**: It is possible to load a file from the workspace on the server side (just click onto a file name), or you can load any remote file from URL (see below).
- **URL**: A URL to a remote file to be loaded. If this is specified then the workspace file is ommited.
- **RDF format**: The RDF format is automatically detected from the file extension. But, you can specify the format explicitly.
- **Graph name**: Name for this loaded graph. It must have the URI notation in angle brackets, e.g., <dbpedia> or `<http://dbpedia.org>`.

#### Load dataset

Load dataset (set of quads) from a file in the workspace or from a remote file available via URL. The source is in some RDF format or serialized format and can involve several graphs.

##### Properties

- **Choose a file from the workspace**: It is possible to load a file from the workspace on the server side (just click onto a file name), or you can load any remote file from URL (see below).
- **URL**: A URL to a remote file to be loaded. If this is specified then the workspace file is ommited.
- **RDF format**: The RDF format is automatically detected from the file extension. But, you can specify the format explicitly.

#### Index

Save dataset into the memory index.

##### Properties

- **Use URI prefixes**: 

### Actions

#### Cache

Serialize loaded dataset into a file in the workspace at the server side for later use.

##### Properties

- **Path**: A relative path to a file related to the workspace where the serialized dataset should be saved.

#### Export

Export the loaded and transformed dataset into a file in the workspace in an RDF format.

##### Properties

- **Path**: A relative path to a file related to the workspace where the exported dataset should be saved.
- **RDF format**: The RDF format is automatically detected from the file extension. But, you can specify the format explicitly.

#### Get quads

Get first 10000 quads from the loaded dataset.

#### Get prefixes

Show all prefixes defined in the loaded dataset.

#### Size

Get number of quads from the loaded dataset.

#### Properties

Get all properties and their ranges with sizes.

#### Histogram

Aggregate triples by their parts and show the histogram.

##### Properties

- **Subjecty**: Aggregate quads by subjects.
- **Predicate**: Aggregate quads by predicates.
- **Object**: Aggregate quads by objects.

## Index

### Loading

#### Load index

Load serialized index from a file in the workspace.

##### Properties

- **Choose a file from the workspace**: You can load a serialized index file from the workspace on the server side (just click onto a file name).
- **Partial loading**: If the index is used only for mapping of triple items then the fact indices of triples are not loaded.

### Transformations

#### To dataset

Convert the memory index back to the dataset.

#### Mine

Mine rules from the indexed dataset with user-defined threshold, patterns and constraints. Default mining parameters are MinHeadSize=100, MinHeadCoverage=0.01, MaxRuleLength=3, no patterns, no constraints (only logical rules without constants).

#### Load ruleset

Load serialized ruleset from a file in the workspace.

##### Properties

- **Choose a file from the workspace**: You can load a serialized ruleset file from the workspace on the server side (just click onto a file name).
- **Rules format**: The ruleset format. Default is "Ruleset cache".
- **Parallelism**: If the value is lower than or equal to 0 and greater than 'all available cores' then the parallelism level is set to 'all available cores'.

#### Cache

Serialize loaded index into a file in the workspace at the server side for later use.

##### Properties

- **In-memory**: Choose whether to save all previous transformations into memory or disk.
  - **Cache ID**: The cache identifier in the memory.
- **On-disk**:
  - **Path**: A relative path to a file related to the workspace where the serialized dataset should be saved.
- **Revalidate**: Check this if you want to re-create the cache from the previous transformations.

### Actions

#### Cache

Serialize loaded index into a file in the workspace at the server side for later use.

##### Properties

- **Path**: A relative path to a file related to the workspace where the serialized dataset should be saved.

## Ruleset

### Transformations

#### Filter

Filter all rules by patterns or measure conditions.

##### Properties

- **Patterns**: In this property, you can define several rule patterns. During the filtering phase, each rule must match at least one of the defined patterns.
- **Measures**: Rules filtering by their interest measures values.
  - **Name**:
  - **Value**: Some condition for numerical comparison, e.g, '> 0.5' or '(0.8;0.9]' or '1.0'

#### Shrink

Slice the ruleset (set of rules) with a specified window.

#### Sort

Sort rules by user-defined rules attributes.

#### Cache

Serialize loaded ruleset into a file in the workspace at the server side for later use.

##### Properties

- **In-memory**: Choose whether to save all previous transformations into memory or disk.
  - **Cache ID**: The cache identifier in the memory.
- **On-disk**:
  - **Path**: A relative path to a file related to the workspace where the serialized dataset should be saved.
- **Revalidate**: Check this if you want to re-create the cache from the previous transformations.

#### Predict

Use a rules model to generate/predict triples from the loaded dataset.

#### Prune

From the list of rules take such rules which cover all genereted triples from the input dataset.

##### Properties

- **Strategy**:
- **Data coverage pruning**:
  - **Only functional properties**: Generate only functional properties. That means only one object can be predicted for pair (subject, predicate).
  - **Only existing triples**: If checked, the common CBA strategy will be used. That means we take only such predicted triples, which are contained in the input dataset. This strategy takes maximally as much memory as the number of triples in the input dataset. If false, we take all predicted triples (including triples which are not contained in the input dataset and are newly generated). For deduplication a HashSet is used and therefore the memory may increase unexpectedly because we need to save all unique generated triples into memory..
- **Closed or OnlyBetterDescendant**:
  - **Measure**: Some measure by which to do this pruning strategy.

#### Compute confidence

Compute the standard confidence for all rules and filter them by a minimal threshold value.

##### Properties

- **Name**: CWA, PCA confidence or Lift
- **CWA confidence**:
  - **Min confidence**: A minimal confidence threshold. This operation counts the standard confidence for all rules and filter them by this minimal threshold. The value range is between 0.001 and 1 included. Default value is set to 0.5.
- **PCA confidence**:
  - **Min PCA confidence**: A minimal PCA (Partial Completeness Assumption) confidence threshold. This operation counts the PCA confidence for all rules and filter them by this minimal threshold. The value range is between 0.001 and 1 included. Default value is set to 0.5.
- **Lift**:
  - **Min confidence**: A minimal confidence threshold. This operation first counts the standard confidence for all rules and filter them by this minimal threshold, then it counts the lift measure by the computed cofindence. The value range is between 0.001 and 1 included. Default value is set to 0.5.
- **Top-k**: Get top-k rules with highest confidence. This is the optional parameter.

#### Make clusters

Make clusters from the ruleset by DBScan algorithm.

##### Properties

- **Min neighbours**: Min number of neighbours to form a cluster.
- **Min similarity**: Min similarity between two rules to form a cluster.

#### To graph-aware rules

Attach information about graphs belonging to output rules.

### Actions

#### Instantiate

Instantiate a rule

#### Cache

Serialize loaded ruleset into a file in the workspace at the server side for later use.

##### Properties

- **Path**: A relative path to a file related to the workspace where the serialized dataset should be saved.

#### Export

Export the ruleset into a file in the workspace.

##### Properties

- **Path**: A relative path to a file related to the workspace where the exported rules should be saved.
- **Rules format**: The output format is automatically detected from the file extension (.txt or .json|.rules). But, you can specify the format explicitly. The 'text' format is human readable, but it can not be parsed for other use in RDFRules, e.g. for completion another dataset. If you need to use the ruleset for other purposes, use the 'json' format or the 'cache' process.

#### Get rules

Get first 10000 rules from the ruleset.

#### Size

Get number of rules from the ruleset.