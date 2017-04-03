# transformer

The Calm transformer reads Calm records from DynamoDB, and transforms them
ready for being ingested into Elasticsearch.

It's a Finatra web app with the actor model.  This is the architecture:

## Internal architecture

We have a Kinesis event stream of changes coming from DynamoDB.  The
transformer receives records from this stream, processes them and sends them to the SNS queue
