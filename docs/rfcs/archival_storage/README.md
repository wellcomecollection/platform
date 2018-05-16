# RFC 2: Archival Storage Service

**Last updated: 15 May 2018.**

## Problem statement

We need to provide a service for storing digital assets.

This service should:

*   Ensure the safe, long-term (i.e. decades) storage of our digital assets
*   Support both digitised and ["born-digital"][borndigital] assets
*   Follow industry best-practices around file integrity and audit trails
*   Provide a scalable mechanism for identifying, retrieving, and storing content

[borndigital]: https://en.wikipedia.org/wiki/Born-digital

## Suggested solution

We will build a storage service based on Amazon S3 and DynamoDB.

![archival storage service - page 1](storage_service.png)

-   New assets are uploaded to an Ingest bucket in S3.
    These assets are gzip-compressed files in the [BagIt format][bagit], a Library of Congress standard for storing collections of digital files.

-   The event stream from S3 triggers the Archival Storage Service, which:

    1.  Retrieves a copy of the BagIt file from the Ingest bucket
    2.  Decompresses the BagIt file, and copies it to a short-term Processing bucket.
        It validates the file -- i.e., checks that the contents match those described by the BagIt metadata.
    3.  Assuming the contents are correct, it copies the files to a long-term Storage bucket.
    4.  It compares checksums between the short-term and long-term storage, to check the transfer was successful.
    5.  It creates a storage manifest describing the stored object, and saves the manifest to the Versioned Hybrid Store (a transactional store for large objects using S3 and DynamoDB).

[bagit]: https://en.wikipedia.org/wiki/BagIt

### Ingest

We'll need to consider integrations with other services such as:

- [Archivematica](https://www.archivematica.org/en/) - for born-digital workflow
- [Goobi](https://www.intranda.com/en/digiverso/goobi/goobi-overview/) - for digitisation workflow

These services will need to provide accessions in the BagIt bag format, gzip-compressed and uploaded to an S3 bucket.

### Storage

Assets will be stored on S3, with master assets stored separately from access derivatives. A full set of derivatives will be stored for all assets, which will be used for access. Master assets will be additionally replicated to a second AWS region, with a Glacier lifecycle policy. All asset storage will have S3 versioning enabled.

#### Locations

The storage service will use three S3 buckets:

- Master asset storage (S3 Glacier, Dublin)
- Master asset storage replica (S3 Glacier, Frankfurt)
- Derivative asset storage (S3 IA, Dublin)

Within each bucket, assets will be namespaced by source and shard, e.g.:

- `/digitised/00/b00000100/{bag contents}`
- `/born_digital/00/00-00-00-00/{bag contents}`

#### Assets

Assets will be stored in the above S3 locations inside the BagIt bags that were transferred for ingest. Unlike during transfer, bags will be stored uncompressed in archival storage. BagIt is a standard archival file format: https://tools.ietf.org/html/draft-kunze-bagit-08

> The BagIt specification is organized around the notion of a “bag”. A bag is a named file system directory that minimally contains:
>
> - a “data” directory that includes the payload, or data files that comprise the digital content being preserved. Files can also be placed in subdirectories, but empty directories are not supported
> - at least one manifest file that itemizes the filenames present in the “data” directory, as well as their checksums. The particular checksum algorithm is included as part of the manifest filename. For instance a manifest file with MD5 checksums is named “manifest-md5.txt”
> - a “bagit.txt” file that identifies the directory as a bag, the version of the BagIt specification that it adheres to, and the character encoding used for tag files

From: [BagIt on Wikipedia](https://en.wikipedia.org/wiki/BagIt)

#### Storage manifest

The storage manifest provides a pointer to the stored accession and enough other metadata to provide a consumer with a comprehensive view of the contents of the accession. It is defined using types from a new Storage ontology and serialised using JSON-LD. We may provide an API on top of storage manifests in the future, as part of an authenticated storage API.

The manifest does not contain metadata from the METS files within a bag, it is purely a storage level index. It will contain data from the `bag-info.txt` file and information about where the assets have been stored. METS files will be separately ingested in the catalogue and reporting pipelines.

### Onward processing

The Versioned Hybrid Store which holds the Storage Manifests provides an event stream of updates.

This event stream can be used to trigger downstream tasks, for example:

*   Sending a file for processing in our catalogue pipeline
*   Feeding other indexes (e.g. Elasticsearch) for reporting

The Versioned Hybrid Store also includes the ability to "reindex" the entire data store. This triggers an update event for every item in the data store, allowing you to re-run a downstream pipeline.

## Examples

### Digitised content

Digitised content will be ingested using Goobi, which will provide a bag layout that we define.

#### Bag

```
b22036593/
|-- data
|   |-- b22036593.xml    // mets file
|   \-- objects
|       \-- b22036593_001.jp2
|           ...
|   \-- alto
|       \-- b22036593_001.xml
|           ...
|-- manifest-sha256.txt
|     a20eee40d609a0abeaf126bc7d50364921cc42ffacee3bf20b8d1c9b9c425d6f data/b22036593.xml
|     e68c93a5170837420f63420bd626650b2e665434e520c4a619bf8f630bf56a7e data/objects/b22036593_001.jp2
|     17c0147413b0ba8099b000fc91f8bc4e67ce4f7d69fb5c2be632dfedb84aa502 data/alto/b22036593_001.xml
|     ...
|-- tagmanifest-sha256.txt
|     791ea5eb5503f636b842cb1b1ac2bb578618d4e85d7b6716b4b496ded45cd44e manifest-sha256.txt
|     13f83db60db65c72bf5077662bca91ed7f69405b86e5be4824bb94ca439d56e7 bag-info.txt
|     a39e0c061a400a5488b57a81d877c3aff36d9edd8d811d66060f45f39bf76d37 bagit.txt
|-- bag-info.txt
|     Source-Organization: Intranda GmbH
|     Contact-Email: support@intranda.com
|     External-Description: A account of a voyage to New South Wales    // title
|     Bagging-Date: 2016-08-07
|     External-Identifier: b22036593    // b number
|     Payload-Oxum: 435255.8
|     Internal-Sender-Identifier: 170131    // goobi process id
|     Internal-Sender-Description: 12324_b_b22036593    // goobi process title
\-- bagit.txt
      BagIt-Version: 0.97
      Tag-File-Character-Encoding: UTF-8
```

#### Storage manifest

```
{
  "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
  "type": "Bag",
  "id": "xx-xx-xx-xx",
  "source": {
    "type": "Source",
    "id": "goobi",
    "label": "Goobi"
  },
  "identifiers": [
    {
      "type": "Identifier",
      "scheme": "sierra-system-number",
      "value": "b22036593"
    },
    {
      "type": "Identifier",
      "scheme": "goobi-process-title",
      "value": "12324_b_b22036593"
    },
    {
      "type": "Identifier",
      "scheme": "goobi-process-id",
      "value": "170131"
    }
  ],
  "manifest": {
    "type": "Manifest",
    "checksumAlgorithm": "sha256",
    "files": [
      {
        "type": "File",
        "checksum": "a20eee40d609a0abeaf126bc7d50364921cc42ffacee3bf20b8d1c9b9c425d6f",
        "path": "data/b22036593.xml"
      },
      {
        "type": "File",
        "checksum": "e68c93a5170837420f63420bd626650b2e665434e520c4a619bf8f630bf56a7e",
        "path": "data/objects/b22036593_001.jp2"
      },
      {
        "type": "File",
        "checksum": "17c0147413b0ba8099b000fc91f8bc4e67ce4f7d69fb5c2be632dfedb84aa502",
        "path": "data/alto/b22036593_001.xml"
      }
    ]
  },
  "tagManifest": {
    "type": "Manifest",
    "checksumAlgorithm": "sha256",
    "files": [
      {
        "type": "File",
        "checksum": "791ea5eb5503f636b842cb1b1ac2bb578618d4e85d7b6716b4b496ded45cd44e",
        "path": "manifest-256.txt"
      },
      {
        "type": "File",
        "checksum": "13f83db60db65c72bf5077662bca91ed7f69405b86e5be4824bb94ca439d56e7",
        "path": "bag-info.txt"
      },
      {
        "type": "File",
        "checksum": "a39e0c061a400a5488b57a81d877c3aff36d9edd8d811d66060f45f39bf76d37",
        "path": "bagit.txt"
      }
    ]
  },
  locations: [
    {
      "type": "DigitalLocation",
      "locationType": "s3-master",
      "url": "s3://masterbucket/digitised/b22036593/"
    },
    {
      "type": "DigitalLocation",
      "locationType": "s3-master-replica",
      "url": "s3://replicabucket/digitised/b22036593/"
    },
    {
      "type": "DigitalLocation",
      "locationType": "s3-master",
      "url": "s3://derivativesbucket/digitised/b22036593/"
    }
  ],
  "description": "A account of a voyage to New South Wales",
  "size": 435255.8,
  "createdDate": "2016-08-07T00:00:00Z",
  "lastModifiedDate": "2016-08-07T00:00:00Z",
  "version": 1
}
```

### Born-digital archives

Born-digital archives will be ingested using Archivematica, which has a pre-existing bag layout that we have to adopt.

#### Bag

```
GC253_1046-a2870a2d-5111-403f-b092-45c569ef9476/
|-- data
|   |-- METS.a2870a2d-5111-403f-b092-45c569ef9476.xml    // mets file
|   \-- objects
|       \-- Disc_1/HEART.WRI
|           ...
|   \-- logs
|       \-- ...
|-- manifest-sha256.txt
|     a20eee40d609a0abeaf126bc7d50364921cc42ffacee3bf20b8d1c9b9c425d6f data/METS.a2870a2d-5111-403f-b092-45c569ef9476.xml
|     4dd52bf39d518cf009b776511ce487fe943272d906abf1f56af9dba568f11cc4 data/objects/Disc_1/HEART.WRI
|     ...
|-- tagmanifest-md5.txt
|     452ad4b7d28249102dcac5d5bafe834e bag-info.txt
|     9e5ad981e0d29adc278f6a294b8c2aca bagit.txt
|     3148319ddaf49214944ec357405a8189 manifest-sha256.txt
|-- bag-info.txt
|     Bagging-Date: 2016-08-07
|     Payload-Oxum: 435255.8
\-- bagit.txt
      BagIt-Version: 0.97
      Tag-File-Character-Encoding: UTF-8
```

#### Storage manifest

```
{
  "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
  "type": "Bag",
  "id": "yy-yy-yy-yy",
  "source": {
    "type": "Source",
    "id": "archivematica",
    "label": "Archivematica"
  },
  "identifiers": [
    {
      "type": "Identifier",
      "scheme": "archivematica-aip-identifier",
      "value": "GC253_1046-a2870a2d-5111-403f-b092-45c569ef9476"
    },
    {
      "type": "Identifier",
      "scheme": "archivematica-aip-guid",
      "value": "a2870a2d-5111-403f-b092-45c569ef9476"
    },
    {
      "type": "Identifier",
      "scheme": "calm-alt-ref-no",
      "value": "GC253/1046"
    }
  ],
  "manifest": {
    "type": "Manifest",
    "checksumAlgorithm": "sha256",
    "files": [
      {
        "type": "File",
        "checksum": "a20eee40d609a0abeaf126bc7d50364921cc42ffacee3bf20b8d1c9b9c425d6f",
        "path": "data/METS.a2870a2d-5111-403f-b092-45c569ef9476.xml"
      },
      {
        "type": "File",
        "checksum": "4dd52bf39d518cf009b776511ce487fe943272d906abf1f56af9dba568f11cc4",
        "path": "data/objects/Disc_1/HEART.WRI"
      }
    ]
  },
  "tagManifest": {
    "type": "Manifest",
    "checksumAlgorithm": "md5",
    "files": [
      {
        "type": "File",
        "checksum": "452ad4b7d28249102dcac5d5bafe834e",
        "path": "bagit.txt"
      },
      {
        "type": "File",
        "checksum": "9e5ad981e0d29adc278f6a294b8c2aca",
        "path": "bag-info.txt"
      },
      {
        "type": "File",
        "checksum": "3148319ddaf49214944ec357405a8189",
        "path": "manifest-256.txt"
      }
    ]
  },
  locations: [
    {
      "type": "DigitalLocation",
      "locationType": "s3-master",
      "url": "s3://masterbucket/born_digital/GC253_1046-a2870a2d-5111-403f-b092-45c569ef9476/"
    },
    {
      "type": "DigitalLocation",
      "locationType": "s3-master-replica",
      "url": "s3://replicabucket/born_digital/GC253_1046-a2870a2d-5111-403f-b092-45c569ef9476/"
    },
    {
      "type": "DigitalLocation",
      "locationType": "s3-master",
      "url": "s3://derivativesbucket/born_digital/GC253_1046-a2870a2d-5111-403f-b092-45c569ef9476/"
    }
  ],
  "description": "GC253_1046",
  "size": 435255.8,
  "createdDate": "2016-08-07T00:00:00Z",
  "lastModifiedDate": "2016-08-07T00:00:00Z",
  "version": 1
}
```
