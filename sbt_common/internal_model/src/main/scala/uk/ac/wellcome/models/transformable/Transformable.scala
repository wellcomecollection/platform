package uk.ac.wellcome.models.transformable

import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord
}
import uk.ac.wellcome.utils.JsonUtil._

sealed trait Transformable extends Sourced

case class CalmTransformable(
  sourceId: String,
  RecordType: String,
  AltRefNo: String,
  RefNo: String,
  data: String,
  sourceName: String = "calm"
) extends Transformable

case class CalmTransformableData(
  AccessStatus: Array[String]
)

case class MiroTransformable(sourceId: String,
                             sourceName: String = "miro",
                             MiroCollection: String,
                             data: String)
    extends Transformable

/** Represents a row in the DynamoDB database of "merged" Sierra records;
  * that is, records that contain data for both bibs and
  * their associated items.
  *
  * Fields:
  *
  *   - `id`: the ID of the associated bib record
  *   - `maybeBibData`: data from the associated bib.  This may be None if
  *     we've received an item but haven't had the bib yet.
  *   - `itemData`: a map from item IDs to item records
  *
  */
case class SierraTransformable(
  sourceId: String,
  sourceName: String = "sierra",
  maybeBibData: Option[SierraBibRecord] = None,
  itemData: Map[String, SierraItemRecord] = Map[String, SierraItemRecord]()
) extends Transformable

object SierraTransformable {
  def apply(sourceId: String, bibData: String): SierraTransformable = {
    val bibRecord = fromJson[SierraBibRecord](bibData).get
    SierraTransformable(sourceId = sourceId, maybeBibData = Some(bibRecord))
  }

  def apply(bibRecord: SierraBibRecord): SierraTransformable =
    SierraTransformable(
      sourceId = bibRecord.id,
      maybeBibData = Some(bibRecord))

  def apply(sourceId: String,
            itemRecord: SierraItemRecord): SierraTransformable =
    SierraTransformable(
      sourceId = sourceId,
      itemData = Map(itemRecord.id -> itemRecord))
}
