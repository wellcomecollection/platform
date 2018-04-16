package uk.ac.wellcome.platform.snapshot_convertor.models

case class ConversionJob(privateBucketName: String,
                         privateObjectKey: String,
                         targetBucketName: String,
                         targetObjectKey: String)
