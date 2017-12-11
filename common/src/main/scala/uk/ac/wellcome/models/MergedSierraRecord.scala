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
  itemData: Map[String, SierraItemRecord] = Map(),
  version: Int = 1
) extends Transformable {

  /** Given a new bib record, construct the new merged row that we should
    * insert into the merged database.
    *
    * Returns None if there's nothing to do.
    */
  def mergeBibRecord(record: SierraBibRecord): Option[MergedSierraRecord] = {
    if (record.id != this.id) {
      throw new RuntimeException(
        s"Non-matching bib ids ${record.id} != ${this.id}")
    }

    val isNewerData = this.maybeBibData match {
      case Some(bibData) => record.modifiedDate.isAfter(bibData.modifiedDate)
      case None => true
    }

    if (isNewerData) {
      Some(
        this.copy(
          maybeBibData = Some(record),
          version = this.version + 1
        ))
    } else {
      None
    }
  }

  /** Given a new item record, construct the new merged row that we should
    * insert into the merged database.
    *
    * Returns None if there's nothing to do.
    */
  def mergeItemRecord(record: SierraItemRecord): Option[MergedSierraRecord] = {
    None
  }

  override def transform: Try[Option[Work]] =
    maybeBibData
      .map { bibData =>
        JsonUtil.fromJson[SierraBibData](bibData.data).map { sierraBibData =>
          Some(
            Work(
              title = sierraBibData.title,
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

  def apply(bibRecord: SierraBibRecord, version: Int): MergedSierraRecord =
    MergedSierraRecord(
      id = bibRecord.id,
      maybeBibData = Some(bibRecord),
      version = version
    )
}
