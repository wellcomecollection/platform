package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

case class SierraItemRecord(
  id: String,
  data: String,
  modifiedDate: Instant,
  bibIds: List[String],
  unlinkedBibIds: List[String],
  version: Int = 0
)
