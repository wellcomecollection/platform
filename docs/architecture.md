# architecture

![](ingest_architecture.png)

We have a number of data sources (initially just Calm and Miro but we'll add others).
An adapter ingests all the records into a per-source DynamoDB table, treating
Dynamo as a mirror of the original source.

A transformer runs on the other side of each Dynamo table, and parses out the
fields we want to expose on Elasticsearch.  These parsed records are sent to
per-source SNS topics, which are in turn coalesced into a single SQS queue.

The id_minter:
* reads each item from the SQS queue,
* checks into a DynamoDB table if the item already has an canonical ID,
    * if it doesn't, generates an ID and saves it into the table
* sends the original item with the internal id into a SNS topic

A SQS queue is subsribed to the SNS topic. An ingestor pulls entries from the queue into the Elasticsearch index, which is
then queried by our API.
