# Archival Storage Service

## Problem Statement

In order to provide archivists with reliable long term digital storage of both digitised and "born-digital" assets we need a service that can: 

- Ensure safe long term storage assets of digitised assets
- Follow industry practises around file integrity and audit trails
- Provide a scalable mechinsm for indetifying and retrieving content

## Suggested Solution

We propose to build a storage service based on AWS S3 and DynamoDb. 

![archival storage service - page 1](https://user-images.githubusercontent.com/953792/39753255-0987d2fe-52b6-11e8-97ee-633ac09e1a9e.png)

### Process flow

- Work is uploaded in gzipped BagIt format to S3 to an "Ingest Bucket"
- The S3 event stream triggers an AWS Lambda, translating the event to an SNS notification
- The "Archival Service" picks up work as an [SQS Autoscaling Service](https://github.com/wellcometrust/terraform-modules/tree/master/sqs_autoscaling_service) subscribed to the topic.
- The Archival Service performs the following operations:
  - Obtain a filestream of the uploaded compressed artefact from its ingest location
  - Stream the decompressed artefact to a processing location
  - Confirm that files match those described by the BagIt metadata
  - Move the BagIt folder to a storage location
  - Confirm checksums match from processing location
  - Create a `StorageManifest` describing the stored object

### Ingest

We'll need to consider integrations with other services such as:

- [archivematica](https://www.archivematica.org/en/)
- [goobi](https://www.intranda.com/en/digiverso/goobi/goobi-overview/)

These services will need to provide accessions in the BagIt bag format, gzipped and uploaded to an S3 location.

### Onward processing

The architecture described here makes use of the "[https://github.com/wellcometrust/platform/tree/master/sbt_common/storage/src/main/scala/uk/ac/wellcome/storage/vhs](Versioned Hybrid Store)", so can via a dynamo event stream / lambda / sns mechanism publish update events further downstream to be consumed by the catalogue pipeline, or to feed another search index (like ElasticSearch), reindexing capability is already demonstrated by the Versioned Hybrid Store.

### Terminology

- **Accession**: A BagIt "bag"
- **SQS Autoscaling Service**: An ECS service autoscaling on SQS queue length as defined in https://github.com/wellcometrust/terraform-modules/tree/master/sqs_autoscaling_service
- **Storage manifest**: A file describing the contents of an accession after ingest and containing a pointer to the stored accession.
- **Versioned Hybrid Store**: A set of [https://github.com/wellcometrust/platform/tree/master/sbt_common/storage/src/main/scala/uk/ac/wellcome/storage/vhs](software libraries) wrapping interactions with dynamo and S3 to provide a transactional typed large object store.

### File formats

We propose to use following file formats as mentioned above:

#### Storage Manifest

The storage manifest is intended to provide a pointer to the stored accession, the location of any derivitaves and enough other metadata to provide a consumer with a comprehensive view of the contents of the accession.

```json
{
  "original_location": "s3://bucket/foo/bar",
  "derivative_location": "s3://bucket/foo/bar",
  "title": "Some Title",
  "description": "Some Description",
  "...": "..."
}
```

#### BagIt

An archival file format:

```
The BagIt specification is organized around the notion of a “bag”. A bag is a named file system directory that minimally contains:

- a “data” directory that includes the payload, or data files that comprise the digital content being preserved. Files can also be placed in subdirectories, but empty directories are not supported
- at least one manifest file that itemizes the filenames present in the “data” directory, as well as their checksums. The particular checksum algorithm is included as part of the manifest filename. For instance a manifest file with MD5 checksums is named “manifest-md5.txt”
- a “bagit.txt” file that identifies the directory as a bag, the version of the BagIt specification that it adheres to, and the character encoding used for tag files
```
From: [BagIt](https://en.wikipedia.org/wiki/BagIt)
