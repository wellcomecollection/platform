package uk.ac.wellcome.models

import java.time.Instant

import com.gu.scanamo.DynamoFormat

case class SierraBibRecord(
  id: String,
  data: String,
  modifiedDate: Instant
)

object SierraBibRecord {
  implicit val instantLongFormat =
    DynamoFormat.coercedXmap[Instant, Long, IllegalArgumentException](
      Instant.ofEpochSecond
    )(
      _.getEpochSecond
    )

  def apply(id: String, data: String, modifiedDate: String): SierraBibRecord =
    SierraBibRecord(
      id = id,
      data = data,
      modifiedDate = Instant.parse(modifiedDate)
    )
}
