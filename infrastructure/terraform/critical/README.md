## Critical Infrastructure

Any infrastructure where extreme care must be taken to prevent deletion of data.

This includes:

*   The "source data" tables -- VHS instances containing pipeline data copied
    from the source systems (e.g. Sierra, Miro, Calm).

    Although these are theoretically transient (we could reharvest all the
    data from the source systems), it's annoying to do so.  We keep them
    separate to avoid breaking them by accident.

*   The ID minter database -- the mapping from source system IDs to platform
    canonical IDs.

    We cannot regenerate this easily (IDs are assigned randomly), so we want
    to protect this from accidental changes.

*   Shared infrastructure used by other projects.

### Elastic Cloud Snapshots

This repo includes the S3 bucket where snapshots from our cloud hosted Elasticsearch provider (Elastic Cloud) are stored for the Catalogue API.

This repository needs to be registered with any new cluster that is created.

See: https://www.elastic.co/guide/en/cloud/current/ec-aws-custom-repository.html

The access key id and (encrypted) secret are available from the terraform outputs for the shared stack.

To decrypt the secret for use you will need to base64 decode the secret output and decode it using the `wellcomedigitalplatform` secret key available via Keybase.