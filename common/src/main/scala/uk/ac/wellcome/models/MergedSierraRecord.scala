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

  def unlinkItemRecord(itemRecord: SierraItemRecord): MergedSierraRecord = {
    if (!itemRecord.unlinkedBibIds.contains(this.id)) {
      throw new RuntimeException(
        s"Non-matching bib id ${this.id} in item unlink bibs ${itemRecord.unlinkedBibIds}")
    }

    val itemData: Map[String, SierraItemRecord] = this.itemData
      .filterNot {
        case (id, currentItemRecord) => {
          val matchesCurrentItemRecord = id == itemRecord.id

          val modifiedAfter = itemRecord.modifiedDate.isAfter(
            currentItemRecord.modifiedDate
          )

          matchesCurrentItemRecord && modifiedAfter
        }
      }

    this.copy(itemData = itemData)
  }

  override def transform: Try[Option[Work]] =
    maybeBibData
      .map { bibData =>
        JsonUtil.fromJson[SierraBibData](bibData.data).map { sierraBibData =>
          Some(Work(
            title = sierraBibData.title,
            sourceIdentifier = SourceIdentifier(
              identifierScheme = IdentifierSchemes.sierraSystemNumber,
              sierraBibData.id
            ),
            identifiers = List(
              SourceIdentifier(
                identifierScheme = IdentifierSchemes.sierraSystemNumber,
                sierraBibData.id
              )
            )
          ))
        }
      }
      .getOrElse(Success(None))
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
