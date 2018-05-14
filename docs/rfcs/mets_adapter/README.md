# METS Adapter

## Background

Wellcome Collection digitises a great number of works which need to be surfaced as part of the digital catalogue.

As part of the digitisation process, we create [Metadata Encoding & Transmission Standard (METS)][loc_mets] files.

METS is a Library of Congress metadata standard:

> The METS schema is a standard for encoding descriptive, administrative, and structural metadata regarding objects within a digital library, expressed using the XML schema language of the World Wide Web Consortium. The standard is maintained in the Network Development and MARC Standards Office of the Library of Congress, and is being developed as an initiative of the Digital Library Federation. — [Library of Congress website, retrieved 14 May 2018][loc_mets]

Wellcome uses the digitisation workflow software [Goobi](https://www.intranda.com/en/digiverso/goobi/goobi-overview/) to produce METS files for digitised content. METS files currently reside in S3, and new files are written as they are created.

[loc_mets]: http://www.loc.gov/standards/mets/

## Problem Statement

We need to ingest METS files created by our instance of Goobi and merge their information into Works.

## Suggested Solution

![overview](overview.png)

METS is an XML format.
Since the catalogue pipeline can only process JSON, we will convert the METS to JSON.

### Process flow

We propose to read from an S3 event stream for object creation and update events as follows:

- **Upload** of METS file from Goobi to S3
- **Update event** feeds an SQS queue with jobs
- METS Adapter ([SQS Autoscaling Service](https://github.com/wellcometrust/terraform-modules/tree/master/sqs_autoscaling_service)) **polls queue**.
- **Convert to JSON** and store in an instance of the versioned hybrid store (VHS).
- **PUT** records to an instance of the versioned hybrid store (VHS) and perform conditional updates on the VHS, based on last updated time of METS file.
- From there METS will flow into the **catalogue pipeline**.
