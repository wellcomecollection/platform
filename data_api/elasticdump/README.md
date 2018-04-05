# elasticdump

This service takes snapshots of our Elasticsearch indexes with [elasticsearch-dump][esdump].

It receives SQS messages (currently any format is supported), and uploads the snapshot to an S3 bucket.  Currently it receives all configuration from environment variables.

[esdump]: https://github.com/taskrabbit/elasticsearch-dump
