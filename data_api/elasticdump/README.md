# elasticdump

This service takes snapshots of our Elasticsearch indexes with [elasticsearch-dump][esdump].

It receives SQS messages (currently any format is supported), and uploads the snapshot to an S3 bucket.  Currently it receives all configuration from environment variables.

[esdump]: https://github.com/taskrabbit/elasticsearch-dump

## Local development

If you want to test the image locally, run the following command from the root of the repo:

```console
$ make elasticdump-build
$ ./docker_run.py --aws --\
    --publish 9000:9000 \
    --env sqs_queue_url={queue_url} \
    --env es_name={name} \
    --env es_index={index} \
    --env es_username={username} \
    --env es_password={password} \
    --env es_region=eu-west-1 \
    --env es_port=9243 \
    --e upload_bucket={bucket} \
    elasticdump
```

## Implementation note

Unlike our Scala applications, elasticdump only reads a single SQS message from the queue, then exits when processed.
It relies on ECS to reschedule it if there are more messages to be read.
