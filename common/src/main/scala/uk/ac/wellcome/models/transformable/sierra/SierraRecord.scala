package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

case class SierraRecord(id: String, data: String, modifiedDate: Instant)
object SierraRecord {
  def apply(id: String, data: String, modifiedDate: String): SierraRecord =
    SierraRecord(
      id = id,
      data = data,
      modifiedDate = Instant.parse(modifiedDate)
    )
}
