# METS Adapter

## Background

The Wellcome Collection digitises a great number of Works which need to be surfaced as part of the digital catalogue.

The process of digitisation results in the creation of [Metadata Encoding & Transmission Standard (METS)](http://www.loc.gov/standards/mets/) files.

METS is a Library of Congress metadata standard:

```
The METS schema is a standard for encoding descriptive, administrative, and structural metadata regarding objects within a digital library, expressed using the XML schema language of the World Wide Web Consortium. The standard is maintained in the Network Development and MARC Standards Office of the Library of Congress, and is being developed as an initiative of the Digital Library Federation.
```

Wellcome uses the digitisation workflow software [Goobi](https://www.intranda.com/en/digiverso/goobi/goobi-overview/) to produce METS files for digitised content. METS files currently reside in S3, and new files are written as they are created.

## Problem Statement

We need to ingest METS files created by our institutions instance of Goobi and merge their information into Works.

## Suggested Solution

![overview](overview.png)

METS is an XML format, so we will convert to JSON as the catalogue pipeline processes only JSON at present.

### Process flow

We propose to read from an S3 event stream for object creation and update events as follows:

- **Upload** of METS file from Goobi to S3
- **Update event** feeds an SQS queue with jobs
- METS Adapter ([SQS Autoscaling Service](https://github.com/wellcometrust/terraform-modules/tree/master/sqs_autoscaling_service)) **polls queue**.
- **Convert to JSON** and store in an instance of the versioned hybrid store (VHS).
- **PUT** records to an instance of the versioned hybrid store (VHS) and perform conditional updates on the VHS, based on last updated time of METS file.
- From there METS will flow into the **catalogue pipeline**.
