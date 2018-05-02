package uk.ac.wellcome.storage.vhs

import uk.ac.wellcome.models.{Id, Versioned}

case class HybridRecord(
  id: String,
  version: Int,
  s3key: String
) extends Versioned
    with Id
