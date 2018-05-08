# METS Adapter

## Problem Statement

We need to ingest METS files created by Goobi and merge their information into works. 

METS files currently reside in S3.

## Suggested Solution

![mets adapter - page 1](https://user-images.githubusercontent.com/953792/39770571-4ff670ca-52e7-11e8-92a9-68474f01588e.png)

### Process flow

We propose to read from an S3 event stream for object creation and update events as follows:

- Update events feed an SQS queue with jobs
- The METS adapter will be an [SQS Autoscaling Service](https://github.com/wellcometrust/terraform-modules/tree/master/sqs_autoscaling_service) polling that queue.
- METS is an XML format, so we will convert to JSON and store in the SourceData instance of the versioned hybrid store (VHS).
- The METS files themselves have a lastUpdated date which we will use to perform conditional updates on the VHS.
- From there work will flow into the catalogue pipeline.

### Terminology
+- **Versioned Hybrid Store**: A set of [https://github.com/wellcometrust/platform/tree/master/sbt_common/storage/src/main/scala/uk/ac/wellcome/storage/vhs](software libraries) wrapping interactions with dynamo and S3 to provide a transactional typed large object store.
