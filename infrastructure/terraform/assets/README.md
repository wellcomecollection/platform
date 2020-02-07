# assets

This is a minimal Terraform stack for managing S3 buckets that contain "assets" -- that is, files or documents that are irretrievable.

We're managing them separately to minimise the risk of data loss caused by a problem in an unrelated Terraform resource.
