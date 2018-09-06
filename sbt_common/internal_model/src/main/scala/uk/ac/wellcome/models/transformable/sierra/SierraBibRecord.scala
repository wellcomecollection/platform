package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

case class SierraBibRecord(
  id: SierraBibNumber,
  data: String,
  modifiedDate: Instant
) extends AbstractSierraRecord

case object SierraBibRecord {
  def apply(id: String, data: String, modifiedDate: Instant): SierraBibRecord =
    SierraBibRecord(
      id = SierraBibNumber(id),
      data = data,
      modifiedDate = modifiedDate
    )
}
