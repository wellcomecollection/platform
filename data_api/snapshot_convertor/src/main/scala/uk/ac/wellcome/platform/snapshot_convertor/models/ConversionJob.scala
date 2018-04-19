package uk.ac.wellcome.platform.snapshot_convertor.models

import uk.ac.wellcome.versions.ApiVersions

case class ConversionJob(publicBucketName: String,
                         publicObjectKey: String,
                         apiVersion: ApiVersions.Value)
