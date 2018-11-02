# RFC 002: Archival Storage Service

**Last updated: 17 October 2018.**

## Problem statement

We need to provide a service for storing digital assets.

This service should:

*   Ensure the safe, long-term (i.e. decades) storage of our digital assets
*   Provide a scalable mechanism for identifying, retrieving, and storing content
*   Follow industry best-practices around file integrity and audit trails
*   Enable us to meet [NDSA Level 4](https://ndsa.org/activities/levels-of-digital-preservation/) for both digitised and ["born-digital"](https://en.wikipedia.org/wiki/Born-digital) assets

## Suggested solution

We will build a storage service based on Amazon S3 and DynamoDB.

![archival storage service - page 1](storageservice-20181017-2.png)

-   New assets are uploaded to an Ingest bucket in S3.
    These assets are gzip-compressed files in the [BagIt format][bagit], a Library of Congress standard for storing collections of digital files.

-   The event stream from S3 triggers the Archival Storage Service, which:

    1.  Retrieves a copy of the BagIt file from the Ingest bucket
    2.  Decompresses the BagIt file and copies it to a short-term Processing bucket
    3.  Validates the file -- i.e., checks that the contents match those described by the BagIt metadata
    4.  Assuming the contents are valid, copies the files to a long-term Storage bucket
    4.  Compares checksums between the short-term and long-term storage, to check the transfer was successful
    5.  Creates a description of the stored bag and saves it to the Versioned Hybrid Store (a transactional store for large objects using S3 and DynamoDB)

[bagit]: https://en.wikipedia.org/wiki/BagIt

### Ingest

We'll need to integrate with other services such as:

- [Archivematica](https://www.archivematica.org/en/) - for born-digital workflow
- [Goobi](https://www.intranda.com/en/digiverso/goobi/goobi-overview/) - for digitisation workflow

These services will need to provide accessions in the BagIt bag format, gzip-compressed and uploaded to an S3 bucket. They should then call an ingest API and provide a callback URL that will be notified when the ingest has succeeded or failed.

### Storage

Assets will be stored on S3, with archival copies stored separately from access copies. A full set of access copies will be stored for all assets, with a Standard-IA storage class. Archival assets will be stored with a Glacier storage class and replicated to Azure Blob Storage with the Archive storage class. All AWS storage will have versioning enabled, but we will only keep the most recent version in Azure as it is intended only for worst case disaster recovery following a complete failure of AWS.

#### Locations

The storage service will use three S3 buckets:

- Archival asset storage (AWS S3 Glacier, Dublin)
- Archival asset storage replica (Azure Blob Storage Archive, Netherlands)
- Access asset storage (AWS S3 IA, Dublin)

Within each bucket, assets will be grouped into related spaces of content and identified by source identifier e.g.:

- `/digitised/b0000000/{bag contents}`
- `/born_digital/0000-0000-0000-0000/{bag contents}`

#### Assets

Assets will be stored in the above locations inside the BagIt bags that were transferred for ingest. Unlike during transfer, bags will be stored uncompressed. BagIt is a standard archival file format: https://tools.ietf.org/html/draft-kunze-bagit-08

> The BagIt specification is organized around the notion of a “bag”. A bag is a named file system directory that minimally contains:
>
> - a “data” directory that includes the payload, or data files that comprise the digital content being preserved. Files can also be placed in subdirectories, but empty directories are not supported
> - at least one manifest file that itemizes the filenames present in the “data” directory, as well as their checksums. The particular checksum algorithm is included as part of the manifest filename. For instance a manifest file with MD5 checksums is named “manifest-md5.txt”
> - a “bagit.txt” file that identifies the directory as a bag, the version of the BagIt specification that it adheres to, and the character encoding used for tag files

From: [BagIt on Wikipedia](https://en.wikipedia.org/wiki/BagIt)

Access copies may be in the same format as the archival copy, or a derivative format if this is more appropriate for access. For example, we would store high bitrate video masters as archival copies and lower bitrate videos as access copies. Any additional preservation formats created during the ingest workflow will be treated in the same way as any other asset, with separate archival and access copies.

#### Bag description

The bag description created by the storage service provides a pointer to the stored accession and enough other metadata to provide a consumer with a comprehensive view of the contents of the accession. It is defined using types from a new Storage ontology and serialised using JSON-LD. We will use this to provide resources that describe stored bags, as part of the authenticated storage API.

This description does not contain metadata from the METS files within a bag, it is purely a storage level index. It will contain data from the `bag-info.txt` file and information about where the assets have been stored. METS files will be separately ingested in the catalogue and reporting pipelines.

### Onward processing

The Versioned Hybrid Store which holds the bag descriptions provides an event stream of updates.

This event stream can be used to trigger downstream tasks, for example:

*   Sending a file for processing in our catalogue pipeline
*   Feeding other indexes (e.g. Elasticsearch) for reporting

The Versioned Hybrid Store also includes the ability to "reindex" the entire data store. This triggers an update event for every item in the data store, allowing you to re-run a downstream pipeline.

## API

The storage service will provide an API that can be used to ingest bags and retrieve information about stored bags. This API will be available publicly, but require authentication using OAuth. Only trusted applications will be granted access to this API.

API base path: `https://api.wellcomecollection.org/storage/v1`

### Authentication

All API endpoints must require authentication using OAuth 2.0. In the first instance, the only supported OAuth grant type will be client credentials.

Clients must first request a time-limited token from the auth service, using a client ID and secret that we will provide:

```http
POST /oauth2/token
Host: auth.wellcomecollection.org

grant_type=client_credentials
&client_id=xxxxxxxxxx
&client_secret=xxxxxxxxxx
```

This will return an access token:

```
Content-Type: application/json
Cache-Control: no-store
Pragma: no-cache

{
  "access_token": "MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3",
  "token_type":" bearer",
  "expires_in": 3600
}
```

This token must be provided on all subsequent requests in the Authorization header:

```http
Authentication: Bearer MTQ0NjJkZmQ5OTM2NDE1ZTZjNGZmZjI3
```

### Ingests

Request:

```http
POST /ingests
Content-Type: application/json

{
  "type": "Ingest",
  "ingestType": {
    "id": "create",
    "type": "IngestType"
  },
  "space": {
    "id": "space-id",
    "type": "Space"
  },
  "sourceLocation": {
    "type": "Location",
    "provider": {
      "type": "Provider",
      "id": "aws-s3-standard"
    },
    "bucket": "source-bucket",
    "path": "/source-path/source-bag.zip"
  },
  "callback": {
    "type": "Callback",
    "url": "https://workflow.wellcomecollection.org/callback?id=b1234567"
  }
}
```

Response:

```http
201 CREATED
Location: /ingests/{id}
```

Request:

```http
GET /ingests/{id}
```

Response:

```json
{
  "@context": "https://api.wellcomecollection.org/storage/v1/context.json",
  "id": "{guid}",
  "type": "Ingest",
  "ingestType": {
    "id": "create",
    "type": "IngestType"
  },
  "space": {
    "id": "space-id",
    "type": "Space"
  },
  "bag": {
    "id": "{id}",
    "type": "Bag"
  },
  "status": {
    "id": "processing|failure|success",
    "type": "Status"
  },
  "sourceLocation": {
    "type": "Location",
    "provider": {
      "type": "Provider",
      "id": "aws-s3-standard"
    },
    "bucket": "source-bucket",
    "path": "/source-path/source-bag.zip"
  },
  "callback": {
    "type": "Callback",
    "url": "https://workflow.wellcomecollection.org/callback?id=b1234567",
    "status": {
      "id": "processing|failure|success",
      "type": "Status"
    }
  },
  "events": [
    {
      "type": "ProgressEvent",
      "createdDate": "2018-10-10T10:00:00Z",
      "description": "Description of event"
    }
  ]
}
```

### Bags

Request:

```http
GET /bags/{space}/{source-id}

```

Response:

See examples below

## Examples

### Digitised content

Digitised content will be ingested using Goobi, which should provide the bag layout defined below.

#### Bag

```
b24923333/
|-- data
|   |-- b24923333.xml      // mets "anchor" file for multiple manifestation
|  [|-- b24923333_001.xml  // mets file for vol 1]
|  [|-- b24923333_002.xml  // mets file for vol 2]
|   \-- objects
|      [\-- b24923333_001_001.jp2 // first image for vol 1]
|      [\-- b24923333_001_002.jp2 // second image for vol 1]
|           ...
|      [\-- b24923333_002_001.jp2 // first image for vol 2]
|      [\-- b24923333_002_002.jp2 // second image for vol 2]
|           ...
|   \-- alto
|      [\-- b24923333_001_001.xml // text for image 1 vol 1]
|      [\-- b24923333_001_002.xml // text for image 2 vol 1]
|           ...
|      [\-- b24923333_002_001.xml // text for image 1 vol 2]
|      [\-- b24923333_002_002.xml // text for image 2 vol 2]
|           ...
|-- manifest-sha256.txt
|     a20eee40d609a0abeaf126bc7d50364921cc42ffacee3bf20b8d1c9b9c425d6f data/b24923333.xml
|     e68c93a5170837420f63420bd626650b2e665434e520c4a619bf8f630bf56a7e data/objects/b24923333_001.jp2
|     17c0147413b0ba8099b000fc91f8bc4e67ce4f7d69fb5c2be632dfedb84aa502 data/alto/b24923333_001.xml
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
|     External-Identifier: b24923333    // b number
|     Payload-Oxum: 435255.8
|     Internal-Sender-Identifier: 170131    // goobi process id
|     Internal-Sender-Description: 12324_b_b24923333    // goobi process title
\-- bagit.txt
      BagIt-Version: 0.97
      Tag-File-Character-Encoding: UTF-8
```

#### METS

The existing METS structure should be change to reflect the following. The main change is removing data from Preservica and replacing it with PREMIS object metadata.

```xml
<?xml version='1.0' encoding='utf-8'?>
<mets:mets xmlns:dv="http://dfg-viewer.de/" xmlns:mets="http://www.loc.gov/METS/" xmlns:mods="http://www.loc.gov/mods/v3" xmlns:premis="http://www.loc.gov/premis/v3" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.loc.gov/mods/v3 http://www.loc.gov/standards/mods/v3/mods-3-5.xsd http://www.loc.gov/METS/ http://www.loc.gov/standards/mets/mets.xsd http://www.loc.gov/standards/premis/ http://www.loc.gov/standards/premis/v2/premis-v2-0.xsd http://www.loc.gov/standards/mix/ http://www.loc.gov/standards/mix/mix.xsd">
  <mets:metsHdr CREATEDATE="2016-01-06T07:36:48">
    <mets:agent OTHERTYPE="SOFTWARE" ROLE="CREATOR" TYPE="OTHER">
      <mets:name>Goobi - ugh-1.10-ugh-2.0.0-18-g99df876 - 21−May−2015</mets:name>
      <mets:note>Goobi</mets:note>
    </mets:agent>
  </mets:metsHdr>
  <mets:dmdSec ID="DMDLOG_0000">
    <mets:mdWrap MDTYPE="MODS"><!-- no change --></mets:dmdSec>
  <mets:dmdSec ID="DMDPHYS_0000"><!-- no change --></mets:dmdSec>
  <mets:amdSec ID="AMD"><!-- remove techMD for deliverable unit, so first file is now AMD_0001 -->
    <mets:techMD ID="AMD_0001">
      <mets:mdWrap MDTYPE="OTHER" MIMETYPE="text/xml">
        <mets:xmlData><!-- replace Preservica data with PREMIS object as below -->
          <premis:object version="3.0" xsi:schemaLocation="http://www.loc.gov/premis/v3 http://www.loc.gov/standards/premis/v3/premis.xsd" xsi:type="premis:file">
            <premis:objectIdentifier>
              <premis:objectIdentifierType>local</premis:objectIdentifierType>
              <premis:objectIdentifierValue>b24923333_0001.jp2</premis:objectIdentifierValue>
              </premis:objectIdentifier>
            <premis:significantProperties>
              <premis:significantPropertiesType>ImageHeight</premis:significantPropertiesType>
              <premis:significantPropertiesValue>4378</premis:significantPropertiesValue>
              </premis:significantProperties>
            <premis:significantProperties>
              <premis:significantPropertiesType>ImageWidth</premis:significantPropertiesType>
              <premis:significantPropertiesValue>2816</premis:significantPropertiesValue>
              </premis:significantProperties>
            <premis:objectCharacteristics>
              <premis:compositionLevel />
              <premis:fixity>
                <premis:messageDigestAlgorithm>SHA-256</premis:messageDigestAlgorithm>
                <premis:messageDigest>0adcae8b53ba8af8d6fef0c1517ef822f0d0c3a7</premis:messageDigest>
                </premis:fixity>
              <premis:size>310448</premis:size>
              <premis:format>
                <premis:formatDesignation>
                  <premis:formatName>JP2 (JPEG 2000 part 1)</premis:formatName>
                  </premis:formatDesignation>
                <premis:formatRegistry>
                  <premis:formatRegistryName>PRONOM</premis:formatRegistryName>
                  <premis:formatRegistryKey>x-fmt/392</premis:formatRegistryKey>
                  </premis:formatRegistry>
                </premis:format>
              </premis:objectCharacteristics>
            </premis:object>
          </mets:xmlData>
      </mets:mdWrap>
    </mets:techMD>
    <mets:rightsMD ID="RIGHTS"><!-- no change --></mets:rightsMD>
    <mets:digiprovMD ID="DIGIPROV"><!-- no change --></mets:digiprovMD>
  </mets:amdSec>
  <mets:fileSec>
    <mets:fileGrp USE="OBJECTS"><!-- change USE from SDB to OBJECTS -->
      <mets:file ID="FILE_0001_OBJECTS" MIMETYPE="image/jp2"><!-- change SDB suffix to OBJECTS -->
        <mets:FLocat LOCTYPE="URL" xlink:href="objects/b22454408_0001.jp2" /><!-- remove CHECKSUM -->
      </mets:file>
    </mets:fileGrp>
  </mets:fileSec>
  <mets:structMap TYPE="LOGICAL"><!-- no change --></mets:structMap>
  <mets:structMap TYPE="PHYSICAL"><!-- no change other than reflecting new IDs --></mets:structMap>
  <mets:structLink><!-- no change --></mets:structLink>
</mets:mets>
```

#### API

Request:

```http
GET /bags/digitised/b24923333
```

Response:

```json
{
  "@context": "https://api.wellcomecollection.org/bags/v1/context.json",
  "type": "Bag",
  "id": "digitised/b24923333",
  "space": {
    "id": "digitised",
    "type": "Space"
  },
  "info": {
    "type": "BagInfo",
    "externalIdentifier": "b24923333",
    "externalDescription": "A account of a voyage to New South Wales",
    "internalSenderIdentifer": "170131",
    "internalSenderDescription": "12324_b_b24923333",
    "sourceOrganisation": "Intranda GmbH",
    "payloadOxum": 435255.3,
    "baggingDate": "2016-08-07",    
  },
  "manifest": {
    "type": "BagManifest",
    "checksumAlgorithm": "sha256",
    "files": [
      {
        "type": "File",
        "checksum": "a20eee40d609a0abeaf126bc7d50364921cc42ffacee3bf20b8d1c9b9c425d6f",
        "path": "data/b24923333.xml"
      },
      {
        "type": "File",
        "checksum": "e68c93a5170837420f63420bd626650b2e665434e520c4a619bf8f630bf56a7e",
        "path": "data/objects/b24923333_001.jp2"
      },
      {
        "type": "File",
        "checksum": "17c0147413b0ba8099b000fc91f8bc4e67ce4f7d69fb5c2be632dfedb84aa502",
        "path": "data/alto/b24923333_001.xml"
      }
    ]
  },
  "tagManifest": {
    "type": "BagManifest",
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
  "accessLocation": {
    "type": "Location",
    "provider": {
      "type": "Provider",
      "id": "aws-s3-ia",
      "label": "AWS S3 - Infrequent Access"
    },
    "bucket": "bucketname",
    "path": "/digitised/b24923333",
    "url": "http://bucketname.s3-eu-west-1.amazonaws.com/digitised/b24923333"
  },
  "archiveLocations": [
    {
      "type": "Location",
      "provider": {
        "type": "Provider",
        "id": "aws-s3-glacier",
        "label": "AWS S3 - Glacier"
      },
      "bucket": "bucketname",
      "path": "/digitised/b24923333",
      "url": "http://bucketname.s3-eu-west-1.amazonaws.com/digitised/b24923333"
    },
    {
      "type": "Location",
      "provider": {
        "type": "Provider",
        "id": "azure-blob-archive",
        "label": "Azure Blob Storage - Archive"
      },
      "bucket": "bucketname",
      "path": "/digitised/b24923333",
      "url": "https://accountname.blob.core.windows.net/bucketname/digitised/b24923333"
    }
  ],
  "createdDate": "2016-08-07T00:00:00Z",
  "lastModifiedDate": "2016-08-07T00:00:00Z"
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
|     External-Identifier: a2870a2d-5111-403f-b092-45c569ef9476
|     External-Description: AIP title
|     Bagging-Date: 2016-08-07
|     Payload-Oxum: 435255.8
\-- bagit.txt
      BagIt-Version: 0.97
      Tag-File-Character-Encoding: UTF-8
```

#### METS

The METS file will be as provided out of the box by Archivematica.

#### API

Request:

```http
GET /bags/born_digital/yy-yy-yy-yy
```

Response:

```json
{
  "@context": "https://api.wellcomecollection.org/bags/v1/context.json",
  "type": "Bag",
  "id": "born_digital/yy-yy-yy-yy",
  "space": {
    "id": "born_digital",
    "type": "Space"
  },
  "info": {
    "type": "BagInfo",
    "externalIdentifier": "a2870a2d-5111-403f-b092-45c569ef9476",
    "externalDescription": "AIP title",
    "payloadOxum": 435255.2,
    "baggingDate": "2016-08-07",    
  },
  "manifest": {
    "type": "BagManifest",
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
    "type": "BagManifest",
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
  "accessLocation": {
    "type": "Location",
    "provider": {
      "type": "Provider",
      "id": "aws-s3-ia",
      "label": "AWS S3 - Infrequent Access"
    },
    "bucket": "bucketname",
    "path": "/born_digital/GC253_1046-a2870a2d-5111-403f-b092-45c569ef9476",
    "url": "http://bucketname.s3-eu-west-1.amazonaws.com/born_digital/GC253_1046-a2870a2d-5111-403f-b092-45c569ef9476"
  },
  "archiveLocations": [
    {
      "type": "Location",
      "provider": {
        "type": "Provider",
        "id": "aws-s3-glacier",
        "label": "AWS S3 - Glacier"
      },
      "bucket": "bucketname",
      "path": "/born_digital/GC253_1046-a2870a2d-5111-403f-b092-45c569ef9476",
      "url": "http://bucketname.s3-eu-west-1.amazonaws.com/born_digital/GC253_1046-a2870a2d-5111-403f-b092-45c569ef9476"
    },
    {
      "type": "Location",
      "provider": {
        "type": "Provider",
        "id": "azure-blob-archive",
        "label": "Azure Blob Storage - Archive"
      },
      "bucket": "bucketname",
      "path": "/born_digital/GC253_1046-a2870a2d-5111-403f-b092-45c569ef9476",
      "url": "https://accountname.blob.core.windows.net/bucketname/born_digital/GC253_1046-a2870a2d-5111-403f-b092-45c569ef9476"
    }
  ],
  "createdDate": "2016-08-07T00:00:00Z",
  "lastModifiedDate": "2016-08-07T00:00:00Z"
}
```
