# solr-reindexer
Reindexes documents from a Solr query to a destination collection. Quick tutorial [here](https://sematext.com/blog/solr-reindexer-quick-way-to-reindex-to-a-new-collection/).
## Usage
Download the uber-jar from [releases](https://github.com/sematext/solr-reindexer/releases) and run it with Java (11+). Here's an example with all the options:
```
java -jar solr-reindexer.jar\
 -sourceCollection my_collection_v1\
 -targetCollection my_collection_v2\ 
 -uniqueKey id\
 -sourceZkAddress localhost:9983,localhost:2181\
 -targetZkAddress zoo1:2181,zoo2:2181\
 -skipFields _version_,text\
 -numWriteThreads 2\
 -queueSize 10000\
 -retries 7\
 -retryInterval 2000\
 -query "isDeleted:false AND isIgnored:false"\
 -rows 100
```

Only `sourceCollection` and `targetCollection` are mandatory.
The rest are:
- `uniqueKey`: we use a cursor to go over the data. The cursor requires to sort on the `uniqueKey` defined in the schema, which in turn defaults to `id`
- `sourceZkAddress` and `targetZkAddress`: the Zookeeper host:port for SolrCloud (source and destination). If there are more, comma-separate them
- `skipFields`: we reindex all the stored and docValues fields by default. But some may be skipped, like the default `_version_` (which will break the reindex because it will cause a version conflict) or copyFields that are also stored (they'll duplicate the values, because you'll redo the copyField operation). Comma-separate multiple fields
- `retries` and `retryInterval`: if we encounter an exception, we wait for `retryInterval` millis and retry up to `retries` times
- `queueSize`: the reader thread writes into an in-memory queue of this size (in pages, see `rows` below for page size). Defaults to 100
- `numWriteThreads`: this many threads consume from the in-memory queue, writing to the target collection. Defaults to 2
- `query`: you may not want to reindex everything with the default `*:*`
- `rows`: we read one page of this size at a time. We also write one batch of this size at a time. Typically, the best performance is around 1MB per batch. Default is 1000 rows per page/batch

## Parallelizing and other performance tips

You can start multiple instances of the reindexer, one per shard, by specifying `-sourceShards shard1` for one instance, `-sourceShards shard2` for another, etc.

You can also group N shards per reindexer by saying `-sourceShards shard1,shard2...` you get it, by comma-separating values.

Typically, the bottleneck is reading. You'll want to run the reindexer close to the source. The default of 2 write threads should keep up, unless the destination (or the network to it) is slow.

## Contributing
Feel free to clone the repository, import it as a Gradle project, and add features.

To build the uber-jar, use `gradle jar`.

Tentative roadmap:
- authentication support
- supporting non-SolrCloud
- using Export instead of Cursor
