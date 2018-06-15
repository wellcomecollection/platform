package uk.ac.wellcome.storage.vhs

import uk.ac.wellcome.models.Id

case class HybridRecord(
  id: String,
  version: Int,
  s3key: String
) extends Id
