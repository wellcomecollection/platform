package uk.ac.wellcome.models.transformable

import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.models.transformable.sierra.{SierraBibNumber, SierraBibRecord, SierraItemNumber, SierraItemRecord}

sealed trait Transformable extends Sourced

case class MiroTransformable(sourceId: String,
                             MiroCollection: String,
                             data: String)
    extends Transformable {
  val sourceName = "miro"
}

case class SierraTransformable(
  sierraId: SierraBibNumber,
  maybeBibRecord: Option[SierraBibRecord] = None,
  itemRecords: Map[SierraItemNumber, SierraItemRecord] = Map()
) extends Transformable {
  val sourceId: String = sierraId.withoutCheckDigit
  val sourceName = "sierra"
}

object SierraTransformable {
  def apply(bibRecord: SierraBibRecord): SierraTransformable =
    SierraTransformable(sierraId = bibRecord.id, maybeBibRecord = Some(bibRecord))
}
