package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

trait AbstractSierraRecord {
  val id: SierraRecordNumber
  val data: String
  val modifiedDate: Instant
}
