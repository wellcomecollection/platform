package uk.ac.wellcome.platform.snapshot_convertor.models

import uk.ac.wellcome.platform.snapshot_convertor.versions.ModelVersions

case class ConversionJob(privateBucketName: String,
                         privateObjectKey: String,
                         publicBucketName: String,
                         publicObjectKey: String,
                         modelVersion: ModelVersions.Value)
