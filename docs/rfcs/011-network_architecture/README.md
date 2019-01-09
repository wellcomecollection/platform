# RFC 011: Network Architecture

**Last updated: 09 January 2019.**

## Background

As the number of Wellcome Collection services grows and integrates with 3rd parties, good practise with respect to network infrastructure is required for effective security, maintenance and scalability.

## Problem statment

There are currently a number of products looked after by the Digital Platform team. The estate has grown organically and the underlying architecture reflects that growth rather than best practise in all cases.

There are a number of development teams in addition to the Digital Platform team who require access to do software development and operations work.

### The current estate

The services we are currently resonsible for are as follows:

- Workflow:
  - [Goobi](https://www.intranda.com/en/digiverso/goobi/goobi-overview/): digitisation workflow services, hosted on-site at Wellcome and in AWS. Maintained by Intranda.
  - [Archivematica](https://www.archivematica.org/en/): born-digital workflow service, hosted in AWS.
- Catalogue:
  - [Catalogue API](https://developers.wellcomecollection.org/catalogue): Wellcome catalogue API, hosted in AWS & with ElasticCloud.
  - [Catalogue Pipeline](https://github.com/wellcometrust/platform/tree/master/catalogue_pipeline): Wellcome catalogue ingest pipeline, hosted in AWS.
  - [Data API](https://developers.wellcomecollection.org/datasets): large Wellcome catalogue datasets, hosted in AWS.
  - Catalogue Adapter services: hosted in AWS, syncing data from services hosted on-site and AWS.
  - IIIF Services:
    - [Image API](https://developers.wellcomecollection.org/iiif): hosted in AWS.
    - [Presentation API](https://dlcs.info/): hosted in AWS. Maintained by digirati.
- Storage:
  - [Archival storage service](https://github.com/wellcometrust/platform/tree/master/docs/rfcs/002-archival_storage): long term immutable data storage with audit trail, hosted in AWS, communicating with on-site & AWS hosted services.
- Data science:
  - Managed notebooks & storage: hosted privately in AWS.
  - [Labs](http://labs.wellcomecollection.org/): a collection of public experiments, hosted in AWS.
- Monitoring:
  - [Grafana](https://monitoring.wellcomecollection.org): Grafana service with visibility on AWS accounts, osted in AWS.
  - [ECS Dashboard](https://wellcomecollection-platform-dashboard.s3.amazonaws.com/index.html): Dashboard providing visibility on ECS services and deployments.

## Proposed network infrastructure

Networks should be split along project lines, using a consistent IP CIDR scheme that is non-overlapping with other Wellcome infrastructure. Network access to 3rd parties should be made available via a transit VPC.

- `transit-10-90-4-0-23`: 10.90.4.0/23 - Transit VPC: IP range within Wellcome internal network, contains VPN connection to Wellcome via internal firewall. (owned by Platform AWS account)
- `storage-172-30-0-0-16`: 172.30.0.0/16 - Storage service infrastructure (owned by Storage AWS account)
- `monitoring-172-28-0-0-16`: 172.28.0.0/16 - Monitoring infrastructure (owned by Platform AWS account)
- `datascience-172-27-0-0-16`: 172.26.0.0/16 - Data science infrastructure & Labs (owned by Collection Data AWS account)
- `catalogue-172-31-0-0-16`: 172.31.0.0/16 - Catalogue service infrastructure (owned by Catalogue AWS account)

The default VPC has been removed.
