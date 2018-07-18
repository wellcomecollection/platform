package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

case class SierraBibRecord(
  id: SierraRecordNumber,
  data: String,
  modifiedDate: Instant
)

object SierraBibRecord {
  def apply(id: String, data: String, modifiedDate: String): SierraBibRecord =
    SierraBibRecord(
      id = id,
      data = data,
      modifiedDate = Instant.parse(modifiedDate)
    )

  def apply(id: String, data: String, modifiedDate: Instant): SierraBibRecord =
    SierraBibRecord(
      id = SierraRecordNumber(id),
      data = data,
      modifiedDate = modifiedDate
    )
}
