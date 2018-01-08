package uk.ac.wellcome.models.transformable

import uk.ac.wellcome.models.transformable.sierra.{SierraBibRecord, SierraItemRecord}
import uk.ac.wellcome.models.{SierraItemRecord, Work}
import uk.ac.wellcome.utils.JsonUtil

import scala.util.Try

sealed trait Transformable

case class CalmTransformableData(
                                  AccessStatus: Array[String]
                                ) extends Transformable

//TODO add some tests around transformation
case class CalmTransformable(
                              RecordID: String,
                              RecordType: String,
                              AltRefNo: String,
                              RefNo: String,
                              data: String,
                              ReindexShard: String = "default",
                              ReindexVersion: Int = 0
                            ) extends Transformable
  with Reindexable[String] {

  val id: ItemIdentifier[String] = ItemIdentifier(
    HashKey("RecordID", RecordID),
    RangeKey("RecordType", RecordType)
  )

}

case class MiroTransformable(MiroID: String,
                             MiroCollection: String,
                             data: String,
                             ReindexShard: String = "default",
                             ReindexVersion: Int = 0)
  extends Transformable
    with Reindexable[String] {

  val id: ItemIdentifier[String] = ItemIdentifier(
    HashKey("MiroID", MiroID),
    RangeKey("MiroCollection", MiroCollection)
  )
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
  *   - `version`: used to track updates to the record in DynamoDB.  The exact
  *     value at any time is unimportant, but it should only ever increase.
  *
  */
case class MergedSierraRecord(
                               id: String,
                               maybeBibData: Option[SierraBibRecord] = None,
                               itemData: Map[String, SierraItemRecord] = Map[String, SierraItemRecord](),
                               version: Int = 0
                             ) extends Transformable


object MergedSierraRecord {
  def apply(id: String, bibData: String): MergedSierraRecord = {
    val bibRecord = JsonUtil.fromJson[SierraBibRecord](bibData).get
    MergedSierraRecord(id = id, maybeBibData = Some(bibRecord))
  }

  def apply(bibRecord: SierraBibRecord): MergedSierraRecord =
    MergedSierraRecord(id = bibRecord.id, maybeBibData = Some(bibRecord))

  def apply(id: String, itemRecord: SierraItemRecord): MergedSierraRecord =
    MergedSierraRecord(id = id, itemData = Map(itemRecord.id -> itemRecord))

  def apply(bibRecord: SierraBibRecord, version: Int): MergedSierraRecord =
    MergedSierraRecord(
      id = bibRecord.id,
      maybeBibData = Some(bibRecord),
      version = version
    )
}