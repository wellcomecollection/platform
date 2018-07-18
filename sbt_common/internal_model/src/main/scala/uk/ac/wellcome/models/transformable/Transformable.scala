package uk.ac.wellcome.models.transformable

import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.models.transformable.sierra.{SierraBibRecord, SierraItemRecord, SierraRecordNumber}
import uk.ac.wellcome.utils.JsonUtil._

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
  maybeBibData: Option[SierraBibRecord] = None,
  itemData: Map[SierraRecordNumber, SierraItemRecord] = Map()
) extends Transformable {
  val sourceId: String = sierraId.withoutCheckDigit
}

object SierraTransformable {
  def apply(sourceId: String, bibData: String): SierraTransformable = {
    val bibRecord = fromJson[SierraBibRecord](bibData).get
    SierraTransformable(sierraId = SierraRecordNumber(sourceId), maybeBibData = Some(bibRecord))
  }

  def apply(bibRecord: SierraBibRecord): SierraTransformable =
    SierraTransformable(sierraId = bibRecord.id, maybeBibData = Some(bibRecord))

  def apply(sourceId: String,
            itemRecord: SierraItemRecord): SierraTransformable =
    SierraTransformable(
      sierraId = SierraRecordNumber(sourceId),
      itemData = Map(itemRecord.id -> itemRecord))
}
