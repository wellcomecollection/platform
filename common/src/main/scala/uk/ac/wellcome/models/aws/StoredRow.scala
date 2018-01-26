package uk.ac.wellcome.dynamo

case class S3Object(
  bucket: String,
  key: String
)

case class StoredRow(
  id: String,
  version: Int,
  s3pointer: S3Object,
  reindexShard: String,
  reindexVersion: Int
)
