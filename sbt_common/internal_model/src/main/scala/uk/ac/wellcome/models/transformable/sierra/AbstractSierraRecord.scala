package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

sealed trait AbstractSierraRecord {
  val id: String
  val data: String
  val modifiedDate: Instant
}
