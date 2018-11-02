# RFC 007: Goobi Upload

**Last updated: 12 October 2018.**

## Problem statement

There are currently four mechanisms in use for uploading assets to Goobi workflows:

- __IA harvesting__: Fully automated download from IA, coordinated by Goobi
- __FTP__: Bulk upload mechanism, automatically matches to existing processes
- __Home directory__: Bulk upload mechanism, requires manual matching to existing processes
- __Hot folder__: Bulk upload for editorial photography, automatically creates new process

Two of these (home directories and hot folders) rely on SMB network shares and a third relies on an insecure, outdated technology that we don't want to run in AWS (FTP).

We want to rationalise this to the following:

- __Web upload__: Built in Goobi web upload for small numbers of files
- __S3__: A new bulk upload mechanism, which automatically matches or creates processes

This allows us to replace the three existing bulk upload mechanisms, one of which is semi-manual, with one that is fully automated and works regardless of network location.

## Suggested solution

### Web

This is already available in Goobi, no changes required.

### S3

#### Package format

Packages should be uploaded to S3 as zip files, one per process. All assets and metadata should be at the root level, in a single directory. Compressing packages into a single file is required to ensure that packages are only processed when completely uploaded.

#### S3 layout

```
s3://wellcomecollection-workflow-upload
|
| /digitised
| /editorial
| /failed
```

#### Processing

##### Initiation

Processing should be triggered automatically by S3 event notifications.

##### Digitised content

Packages placed in the `digitised` prefix should be automatically matched to an existing process.

##### Editorial photography

Packages placed in the `editorial` prefix should automatically create an editorial photography process.

##### Completion

Succesfully processed packages should be deleted from the upload bucket. Failed packages should be moved to the `failed` prefix.