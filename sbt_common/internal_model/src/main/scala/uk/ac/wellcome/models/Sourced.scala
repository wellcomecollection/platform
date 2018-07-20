package uk.ac.wellcome.models

import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber

trait Sourced {
  def sourceId: String
  val sourceName: String
  val id: String = Sourced.id(sourceName, sourceId)
}

object Sourced {
  def id(sourceName: String, sourceId: String) = s"$sourceName/$sourceId"

  def id(sierraId: SierraRecordNumber): String = id(
    sourceName = "sierra",
    sourceId = sierraId.withoutCheckDigit
  )
}
