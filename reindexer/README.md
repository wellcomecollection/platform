# Reindexer

The reindexer pushes data from source data VHS tables in the form of `HybridRecord`s onto a topic.

The `trigger_reindex.py` script generates work for the `reindex_worker` to consume in the form of shard references.

```scala
case class ReindexJob(segment: Int, totalSegments: Int)
```

The `reindex_worker` than uses the DynamoDB [parallel scan](https://docs.aws.amazon.com/amazondynamodb/latest/developerguide/Scan.html#Scan.ParallelScan) functionality to read and push records to an SNS topic.

