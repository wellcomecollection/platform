package uk.ac.wellcome.storage.s3

case class S3Config(
  bucketName: String,
  endpoint: String = "",
  accessKey: String = "",
  secretKey: String = ""
)
