package uk.ac.wellcome.models.aws

case class S3Config(
  bucketName: String,
  endpoint: String = "",
  accessKey: String = "",
  secretKey: String = ""
)
