# solr-reindexer
Reindexes documents from a Solr query to a destination collection
## Usage
Download the uber-jar from [releases](https://github.com/sematext/solr-reindexer/releases) and run it with Java (11+). Here's an example with all the options:
```
java -jar solr-reindexer.jar\
 -sourceCollection my_collection_v1\
 -targetCollection my_collection_v2\ 
 -uniqueKey id\
 -zkAddress localhost:9983,localhost:2181\
 -skipFields _version_,text\
 -retries 7\
 -retryInterval 2000\
 -query "isDeleted:false AND isIgnored:false"\
 -rows 100
```

Only `sourceCollection` and `targetCollection` are mandatory.
The rest are:
- `uniqueKey`: we use a cursor to go over the data. The cursor requires to sort on the `uniqueKey` defined in the schema, which in turn defaults to `id`
- `zkAddress`: the Zookeeper host:port for SolrCloud. If there are more, comma-separate them
- `skipFields`: we reindex all the stored and docValues fields by default. But some may be skipped, like the default `_version_` (which will break the reindex because it will cause a version conflict) or copyFields that are also stored (they'll duplicate the values, because you'll redo the copyField operation). Comma-separate multiple fields
- `retries` and `retryInterval`: if we encounter an exception, we wait for `retryInterval` millis and retry up to `retries` times
- `query`: you may not want to reindex everything with the default `*:*`
- `rows`: we reindex one page at a time. Typically, the best performance is around 1MB per batch. Default is 1000 rows per page/batch

## Contributing
Feel free to clone the repository, import it as a Gradle project, and add features. Some that might be useful:
- support different source and target clusters
- authentication support
- multi-threading writes using a queue
- using multiple parallel cursors (e.g. one per shard)
- supporting non-SolrCloud
- using Export instead of Cursor
