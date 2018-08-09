package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

trait AbstractSierraRecord {
  val id: SierraTypedRecordNumber
  val data: String
  val modifiedDate: Instant
}
