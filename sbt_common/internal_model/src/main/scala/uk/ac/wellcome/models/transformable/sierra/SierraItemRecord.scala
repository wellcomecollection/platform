package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

case class SierraItemRecord(
  id: SierraRecordNumber,
  data: String,
  modifiedDate: Instant,
  bibIds: List[SierraRecordNumber],
  unlinkedBibIds: List[SierraRecordNumber] = List(),
  version: Int = 0
)
