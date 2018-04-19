package uk.ac.wellcome.platform.snapshot_generator.models

import uk.ac.wellcome.versions.ApiVersions

case class SnapshotJob(publicBucketName: String,
                       publicObjectKey: String,
                       apiVersion: ApiVersions.Value)
