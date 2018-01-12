package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

case class SierraBibRecord(
  id: String,
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

  def apply(sierraRecord: SierraRecord): SierraBibRecord = {
    SierraBibRecord(id = sierraRecord.id,
                    data = sierraRecord.data,
                    modifiedDate = sierraRecord.modifiedDate)
  }
}
