# RFC 2: Archival Storage Service

**Last updated: 14 May 2018.**

## Problem Statement

We need to provide a service for storing digital assets.

This service should:

*   Ensure the safe, long-term (i.e. decades) storage of our digital assets
*   Support both digitised and ["born-digital"][borndigital] assets
*   Follow industry best-practices around file integrity and audit trails
*   Provide a scalable mechanism for identifying, retrieving, and storing content

[borndigital]: https://en.wikipedia.org/wiki/Born-digital

## Suggested Solution

We will build a storage service based on Amazon S3 and DynamoDB.

![archival storage service - page 1](storage_service.png)

-   New assets are uploaded to an Ingest bucket in S3.
    These assets are gzip-compressed files in the [Bagit format][bagit], a common format for storing collections of digital files.

-   The event stream from S3 triggers the Archival Storage Service, which:

    1.  Retrieves a copy of the Bagit file from the Ingest bucket
    2.  Decompresses the Bagit file, and copies it to a short-term Processing bucket.
        It validates the file -- i.e., checks that the contents match those described by the Bagit metadata.
    3.  Assuming the contents are correct, it copies the files to a long-term Storage bucket.
    4.  It compares checksums between the short-term and long-term storage, to check the transfer was successful.
    5.  It creates a storage manifest describing the stored object, and saves the manifest to the Versioned Hybrid Store (a transactional store for large objects using S3 and DynamoDB).

[bagit]: https://en.wikipedia.org/wiki/BagIt

### Ingest

We'll need to consider integrations with other services such as:

- [archivematica](https://www.archivematica.org/en/)
- [goobi](https://www.intranda.com/en/digiverso/goobi/goobi-overview/)

These services will need to provide accessions in the BagIt bag format, gzip-compressed and uploaded to an S3 bucket.

### Onward processing

The Versioned Hybrid Store which holds the Storage Manifests provides an event stream of updates.

This event stream can be used to trigger downstream tasks, for example:

*   Sending a file for processing in our catalogue pipeline
*   Feeding a search index (e.g. Elasticsearch)

The Versioned Hybrid Store also includes the ability to "reindex" the entire data store.
This triggers an update event for every item in the data store, allowing you to re-run a downstream pipeline.

### File formats

We propose to use following file formats as mentioned above:

#### Storage Manifest

The storage manifest provides a pointer to the stored accession and enough other metadata to provide a consumer with a comprehensive view of the contents of the accession.

The storage manifest will include a serialization of the `bag-info.txt` file from the BagIt "bag".

```json
{
  "original_location": "s3://bucket/foo/bar",
  "derivative_location": "s3://bucket/foo/bar",
  "title": "Some Title",
  "description": "Some Description",
  "bag_it": {
  "...": "..."
  }
  "...": "..."
}
```

#### BagIt

An archival file format: https://tools.ietf.org/html/draft-kunze-bagit-08

> The BagIt specification is organized around the notion of a “bag”. A bag is a named file system directory that minimally contains:
>
> - a “data” directory that includes the payload, or data files that comprise the digital content being preserved. Files can also be placed in subdirectories, but empty directories are not supported
> - at least one manifest file that itemizes the filenames present in the “data” directory, as well as their checksums. The particular checksum algorithm is included as part of the manifest filename. For instance a manifest file with MD5 checksums is named “manifest-md5.txt”
> - a “bagit.txt” file that identifies the directory as a bag, the version of the BagIt specification that it adheres to, and the character encoding used for tag files

From: [BagIt on Wikipedia](https://en.wikipedia.org/wiki/BagIt)
