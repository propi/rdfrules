# RDFRules: GUI operations documentation

*popis* rdf rules [bobo](https://duckduckgo.com)

## Dataset

### Loading

### Transformations

#### Filter quads

popis popis

##### Properties

- **Filter by (logical OR)**: Defined quad filters. It filters such quads which satisfy defined conditions.
  - **Subject**: Filter for the subject position. If this field is empty then no filter is applied here. The subject must be written in URI format in angle brackets, e.g, `<http://dbpedia.org/resource/Rule>`, or as a prefixed URI, e.g., dbr:Rule. The content is evaluated as a regular expression.
  - **Predicate**: Filter for the predicate position. If this field is empty then no filter is applied here. The predicate must be written in URI format in angle brackets, e.g, `<https://www.w3.org/2000/01/rdf-schema#label>`, or as a prefixed URI, e.g., rdfs:label. The content is evaluated as a regular expression.
  - **Object**: Filter for the object position. If this field is empty then no filter is applied here. The content is evaluated as a regular expression. You can filter resources (with regexp) and literals (with regexp and conditions). Literals can be text, number, boolean or interval. For TEXT, the content must be in double quotation marks. For NUMBER, you can use exact matching or conditions, e.g., '> 10' or intervals [10;80). For BOOLEAN, there are valid only two values true|false. For INTERVAL, you can use only exact matching like this: i[10;50); it must start with 'i' character.
  - **Graph**: Filter for the graph position. If this field is empty then no filter is applied here. The graph must be written in URI format in angle brackets, e.g, `<http://dbpedia.org>`. The content is evaluated as a regular expression.
  - **Negation**: If this field is checked then all defined filters (above) are negated (logical NOT is applied before all filters).

### Actions