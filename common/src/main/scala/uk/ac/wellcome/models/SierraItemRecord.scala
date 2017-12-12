package uk.ac.wellcome.models

import java.time.Instant

import com.gu.scanamo.DynamoFormat

case class SierraItemRecord(
  id: String,
  data: String,
  modifiedDate: Instant,
  bibIds: List[String],
  unlinkedBibIds: List[String] = List()
)

object SierraItemRecord {
  def apply(id: String,
            data: String,
            modifiedDate: String,
            bibIds: List[String]): SierraItemRecord =
    SierraItemRecord(
      id = id,
      data = data,
      modifiedDate = Instant.parse(modifiedDate),
      bibIds = bibIds
    )
  def apply(id: String,
            data: String,
            modifiedDate: String,
            bibIds: List[String],
            unlinkedBibIds: List[String]): SierraItemRecord =
    SierraItemRecord(
      id = id,
      data = data,
      modifiedDate = Instant.parse(modifiedDate),
      bibIds = bibIds,
      unlinkedBibIds = unlinkedBibIds
    )
}
