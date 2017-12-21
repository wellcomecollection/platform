package uk.ac.wellcome.models

import uk.ac.wellcome.models.transformable.Transformable
import uk.ac.wellcome.utils.JsonUtil

import scala.util.{Success, Try}

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
) extends Transformable {

  /** Return the most up-to-date combination of the merged record and the
    * bib record we've just received.
    */
  def mergeBibRecord(record: SierraBibRecord): MergedSierraRecord = {
    if (record.id != this.id) {
      throw new RuntimeException(
        s"Non-matching bib ids ${record.id} != ${this.id}")
    }

    val isNewerData = this.maybeBibData match {
      case Some(bibData) => record.modifiedDate.isAfter(bibData.modifiedDate)
      case None => true
    }

    if (isNewerData) {
      this.copy(maybeBibData = Some(record))
    } else {
      this
    }
  }

  /** Given a new item record, construct the new merged row that we should
    * insert into the merged database.
    *
    * Returns the merged record.
    */
  def mergeItemRecord(itemRecord: SierraItemRecord): MergedSierraRecord = {
    if (!itemRecord.bibIds.contains(this.id)) {
      throw new RuntimeException(
        s"Non-matching bib id ${this.id} in item bib ${itemRecord.bibIds}")
    }

    // We can decide whether to insert the new data in two steps:
    //
    //  - Do we already have any data for this item?  If not, we definitely
    //    need to merge this record.
    //  - If we have existing data, is it newer or older than the update we've
    //    just received?  If the existing data is older, we need to merge the
    //    new record.
    //
    val isNewerData = this.itemData.get(itemRecord.id) match {
      case Some(existing) =>
        itemRecord.modifiedDate.isAfter(existing.modifiedDate)
      case None => true
    }

    if (isNewerData) {
      val itemData = this.itemData + (itemRecord.id -> itemRecord)
      this.copy(itemData = itemData)
    } else {
      this
    }
  }
}

case class SierraBibData(id: String, title: String)

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
