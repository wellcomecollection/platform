package uk.ac.wellcome.models

import java.time.Instant

import com.gu.scanamo.DynamoFormat

case class SierraItemRecord(
  id: String,
  data: String,
  modifiedDate: Instant
)

object SierraItemRecord {
  implicit val instantLongFormat =
    DynamoFormat.coercedXmap[Instant, Long, IllegalArgumentException](
      Instant.ofEpochSecond
    )(
      _.getEpochSecond
    )

  def apply(id: String, data: String, modifiedDate: String): SierraItemRecord =
    SierraItemRecord(
      id = id,
      data = data,
      modifiedDate = Instant.parse(modifiedDate)
    )
}
