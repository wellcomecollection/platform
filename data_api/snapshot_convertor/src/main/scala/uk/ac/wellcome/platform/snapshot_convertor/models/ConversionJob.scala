package uk.ac.wellcome.platform.snapshot_convertor.models

case class ConversionJob(privateBucketName: String,
                         sourceObjectKey: String,
                         targetBucketName: String,
                         targetObjectKey: String)
