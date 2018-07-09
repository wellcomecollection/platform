package uk.ac.wellcome.storage.vhs

case class HybridRecord(
  id: String,
  version: Int,
  s3key: String
)
