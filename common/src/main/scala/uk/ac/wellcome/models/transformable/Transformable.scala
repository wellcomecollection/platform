package uk.ac.wellcome.models.transformable

import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord
}
import io.circe.Decoder
import cats.syntax.functor._
import uk.ac.wellcome.models.Versioned
import uk.ac.wellcome.utils.JsonUtil._

sealed trait Transformable
object Transformable {
  import uk.ac.wellcome.utils.JsonUtil._

  implicit val decodeEvent: Decoder[Transformable] =
    List[Decoder[Transformable]](
      Decoder[CalmTransformable].widen,
      Decoder[SierraTransformable].widen,
      Decoder[MiroTransformable].widen
    ).reduceLeft(_ or _)
}

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

case class CalmTransformableData(
  AccessStatus: Array[String]
) extends Transformable

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
case class SierraTransformable(
  version: Int = 0,
  sourceId: String,
  sourceName: String = "sierra",
  maybeBibData: Option[SierraBibRecord] = None,
  itemData: Map[String, SierraItemRecord] = Map[String, SierraItemRecord]()
) extends Transformable
    with Versioned

object SierraTransformable {
  def apply(sourceId: String, bibData: String): SierraTransformable = {
    val bibRecord = fromJson[SierraBibRecord](bibData).get
    SierraTransformable(sourceId = sourceId, maybeBibData = Some(bibRecord))
  }

  def apply(bibRecord: SierraBibRecord): SierraTransformable =
    SierraTransformable(sourceId = bibRecord.id,
                        maybeBibData = Some(bibRecord))

  def apply(sourceId: String,
            itemRecord: SierraItemRecord): SierraTransformable =
    SierraTransformable(sourceId = sourceId,
                        itemData = Map(itemRecord.id -> itemRecord))

  def apply(bibRecord: SierraBibRecord, version: Int): SierraTransformable =
    SierraTransformable(
      sourceId = bibRecord.id,
      maybeBibData = Some(bibRecord),
      version = version
    )
}
