package uk.ac.wellcome.models.transformable

import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord,
  SierraRecordNumber
}

sealed trait Transformable extends Sourced

case class MiroTransformable(sourceId: String,
                             sourceName: String = "miro",
                             MiroCollection: String,
                             data: String)
    extends Transformable

/** Represents a row in the DynamoDB database of "merged" Sierra records;
  * that is, records that contain data for both bibs and
  * their associated items.
  */
case class SierraTransformable(
  sierraId: SierraRecordNumber,
  sourceName: String = "sierra",
  maybeBibRecord: Option[SierraBibRecord] = None,
  itemRecords: Map[String, SierraItemRecord] = Map()
) extends Transformable {
  val sourceId = sierraId.withoutCheckDigit
}

object SierraTransformable {
  def apply(bibRecord: SierraBibRecord): SierraTransformable =
    SierraTransformable(sierraId = bibRecord.id, maybeBibRecord = Some(bibRecord))
}
