package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

case class SierraBibRecord(
  id: SierraRecordNumber,
  data: String,
  modifiedDate: Instant
)
