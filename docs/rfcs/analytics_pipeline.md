# Analytics Pipeline

## Problem Statement

In order to run analytics and reporting on data from various library sources we need to make source data available in a searchable format.

## Proposed Solution

We propose to add a simple analytics pipeline powered by lambda functions feeding an ElasticSearch cluster.

![analytics pipeline - page 1 2](https://user-images.githubusercontent.com/953792/39773143-0fd21ed8-52ef-11e8-8c53-821f3358b761.png)

### Process flow

The event stream from the SourceData "Versioned Hybrid Store" triggers:

- A lambda which performs a custom transformation on sourcedata making it suitable for ingest into elasticsearch.
  - This lambda will pass a json object and index identifier to SNS
- An ingestion lambda PUTs the object passed to the specified index

It is intended that there may be multiple transformation lambdas, providing custom transforms. There will be one ingestion lambda intended to try and PUT any object to any index specified.

#### Ingestion Lambda proposed message format

The ingestion lambda needs to take a message that configures which index to attempt to add the object to.

```json
{
  "index": "my-index-1",
  "object": {
    "foo": "bar"
  }
}
```

#### Elasticsearch mappings

It is not intended that strict mappings will be provided. It will instead be the job of the transformation to provide representative 


### Terminology

- **Versioned Hybrid Store**: A set of [https://github.com/wellcometrust/platform/tree/master/sbt_common/storage/src/main/scala/uk/ac/wellcome/storage/vhs](software libraries) wrapping interactions with dynamo and S3 to provide a transactional typed large object store.
