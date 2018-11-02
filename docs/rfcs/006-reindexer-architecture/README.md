# RFC 006: Reindexer architecture

**Last updated: 12 October 2018.**

## Problem statement

We store records in tables in DynamoDB.
Downstream applications consume an event stream from the table.

If we want to reprocess the records in a table, we need to trigger an event for every row in the table.
The only way to trigger an event is to modify a row, so we have a pipeline that edits the rows of a table, called the _reindexer_.

We add a new field to the rows in our table, called _reindexVersion_.
This is an integer, incremented for every reindex -- the increment is what triggers the new event.
The reindexer is the only application which edits this field.

## Prior approaches

We've tried a couple of approaches to the reindexer already:

-   A standalone script which can be run locally, e.g. written in Python.

    This is just too slow, as most scripts will only process the table in serial.
    It also requires duplicating our conditional update logic in another language.

    Any new solution needs to work in parallel, which means it's probably a Scala app.

-   A "reindex_worker" that does most of the work, with Lambdas at the edges.
    The exact process is as follows:

    0.  The table is divided into "reindex shards".
        Each row is in a single shard, and each shard contains ~1500 records.

    1.  The user triggers a reindex with a local script.

    2.  The script updates a reindex shard tracker table, which records the desired and current versions.
        Specifically, it increments the desired version on every record in the table.

    3.  A Lambda (the "reindex job creator") gets the event stream from this table, and sends any rows where (current version) < (desired version) to an SQS queue.

        The SQS message is of the form:

            {
                "shardId": "miro/123",
                "desiredVersion": 456
            }

    4.  An ECS service (the "reindex worker") reads messages from this SQS queue.
        It queries a secondary index on the DynamoDB table to find all records in the shard which have a _reindexVersion_ lower than the _desiredVersion_ from the SQS message.

        This returns a list of up to 1500 records in the shard.

    5.  It proceeds to update every record in the shard.
        It has to make an individual PutItem call for each record, as we do conditional updates (locking around the _version_ field) to avoid conflicts.

    6.  When it's finished updating the shard, it sends a completion message to an SNS topic.

    7.  Another Lambda ("complete_reindex") receives the completion message, and updates the current version in the reindex shard tracker table.

    We've seen issues in step 5 -- making individual updates to the table.
    If we hit any sort of DynamoDB error (e.g. throughput limit exceeded, conditional update failure), the batch fails and has to be restarted from scratch.
    In practice, this means a lot of reindex jobs end up on a DLQ, and incomplete.

    Any new solution needs to reduce the number of DynamoDB PutItem calls required to process a single SQS message.

## Proposed solution

We split the reindex worker into two tasks:

1.  Querying DynamoDB to find out which records need reindexing
2.  Performing the PutItem call to do the update

The new flow is as follows:

0.  The table is still divided into "reindex shards", of roughly the same size as before.

1.  The user triggers a reindex with a local script, specifying the exact version they want to reindex to.

2.  The script reads the reindex shard tracker table, but only to get a list of shards.

    For every shard in the table, it sends an SQS message of the form:

        {
            "shardId": "miro/123",
            "desiredVersion": 456
        }

    (We do this in a local script rather than a Lambda to avoid hitting the Lambda timeout.
    The number of shards is typically a few hundred, so it's long enough to risk hitting Lambda limits, but not so long as to be onerous.)

3.  A new ECS service (the "reindex job creator") receives this message.
    It queries DynamoDB to find all records in the shard which need reindexing, then sends a message to a new SQS queue.
    This message is of the form:

        {
            "id": "miro/123",
            "desiredVersion": 456
        }

4.  A new ECS service (the "reindex worker") receives this message.
    It queries DynamoDB to get the current row with this ID, and if it still needs reindexing, it makes the PutItem call to update the row.

    When it's updated the row (or decided it doesn't need updating), it deletes the message from the SQS queue

We can use the reindex worker's DLQ to detect any records which consistently fail to reindex.