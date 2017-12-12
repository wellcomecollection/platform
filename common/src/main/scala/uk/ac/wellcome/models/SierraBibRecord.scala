package uk.ac.wellcome.models

import java.time.Instant

import com.gu.scanamo.DynamoFormat

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
}
