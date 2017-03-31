# transformer

The Calm transformer reads Calm records from DynamoDB, and transforms them
ready for being ingested into Elasticsearch.

It's a Finatra web app with the actor model.  This is the architecture:

![](architecture.png)

## Internal architecture

We have a Kinesis event stream of changes coming from DynamoDB.  The
transformer receives records from this stream, and passes them through
five different actors:

*   The `KinesisDynamoRecordExtractorActor` polls this stream for updates, and
    when it spots new updates on the stream it grabs the raw records and pushes
    them down the stack for processing.

*   The `DynamoCaseClassExtractorActor` checks the Kinesis event is well-formed,
    then extracts the DynamoDB row that was added.  This includes the raw,
    unfiltered Calm data.  (The event includes other metadata that we don't
    care about.)

*   The `TransformActor` takes the raw Calm data from this record, and tidies
    it up to fit the unified model that fits our ontology.  This is the data
    which gets sent to Elasticsearch.

*   The `PublishableMessageActor` takes the tidied data, and packages it for
    sending to SNS.  It doesn't actually send the record to SNS – that's the
    next actor.

*   The `PublisherActor` actually sends the records to SNS, from which they'll
    be picked up by a separate app and sent to Elasticsearch.
