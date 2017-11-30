package uk.ac.wellcome.platform.sierra_to_dynamo.models

import java.time.Instant

import com.gu.scanamo.DynamoFormat

case class SierraRecord(
  id: String,
  data: String,
  modifiedDate: Instant
)

object SierraRecord {
  implicit val instantLongFormat =
    DynamoFormat.coercedXmap[Instant, Long, IllegalArgumentException](
      Instant.ofEpochSecond
    )(
      _.getEpochSecond
    )

  def apply(id: String, data: String, modifiedDate: String): SierraRecord =
    SierraRecord(
      id = id,
      data = data,
      modifiedDate = Instant.parse(modifiedDate)
    )
}
