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
