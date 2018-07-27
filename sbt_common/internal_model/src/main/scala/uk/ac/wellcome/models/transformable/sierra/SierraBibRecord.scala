package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

case class SierraBibRecord(
  id: SierraBibNumber,
  data: String,
  modifiedDate: Instant
) extends AbstractSierraRecord
