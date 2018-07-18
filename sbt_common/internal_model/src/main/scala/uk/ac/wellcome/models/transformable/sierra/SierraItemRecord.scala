package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

case class SierraItemRecord(
  id: SierraRecordNumber,
  data: String,
  modifiedDate: Instant,
  bibIds: List[String],
  unlinkedBibIds: List[String] = List(),
  version: Int = 0
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

  def apply(id: String,
            data: String,
            modifiedDate: Instant,
            bibIds: List[String],
            unlinkedBibIds: List[String] = List(),
            version: Int = 0): SierraItemRecord =
    SierraItemRecord(
      id = SierraRecordNumber(id),
      data = data,
      modifiedDate = modifiedDate,
      bibIds = bibIds,
      unlinkedBibIds = unlinkedBibIds,
      version = version
    )
}
