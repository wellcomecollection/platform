package uk.ac.wellcome.dynamo

case class S3Object(
  bucket: String,
  key: String
)

// TODO: This is an awful name, pick a better one
case class StoredRow(
  id: String,
  version: Int,
  s3pointer: S3Object,
  reindexShard: String,
  reindexVersion: Int
)
