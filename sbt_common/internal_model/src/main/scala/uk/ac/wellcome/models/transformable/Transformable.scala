package uk.ac.wellcome.models.transformable

import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord
}
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
  maybeBibRecord: Option[SierraBibRecord] = None,
  itemData: Map[String, SierraItemRecord] = Map()
) extends Transformable

object SierraTransformable {
  def apply(bibRecord: SierraBibRecord): SierraTransformable =
    SierraTransformable(sourceId = bibRecord.id, maybeBibRecord = Some(bibRecord))
}
