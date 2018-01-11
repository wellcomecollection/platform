package uk.ac.wellcome.models.transformable

import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord
}
import uk.ac.wellcome.utils.JsonUtil
import io.circe.Decoder
import cats.syntax.functor._
import io.circe.generic.extras.auto._

sealed trait Transformable
object Transformable {
  import uk.ac.wellcome.circe._

  implicit val decodeEvent: Decoder[Transformable] =
    List[Decoder[Transformable]](
      Decoder[CalmTransformable].widen,
      Decoder[SierraTransformable].widen,
      Decoder[MiroTransformable].widen
    ).reduceLeft(_ or _)
}

case class CalmTransformableData(
  AccessStatus: Array[String]
) extends Transformable

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
case class SierraTransformable(
  id: String,
  maybeBibData: Option[SierraBibRecord] = None,
  itemData: Map[String, SierraItemRecord] = Map[String, SierraItemRecord](),
  version: Int = 0
) extends Transformable

object SierraTransformable {
  def apply(id: String, bibData: String): SierraTransformable = {
    val bibRecord = JsonUtil.fromJson[SierraBibRecord](bibData).get
    SierraTransformable(id = id, maybeBibData = Some(bibRecord))
  }

  def apply(bibRecord: SierraBibRecord): SierraTransformable =
    SierraTransformable(id = bibRecord.id, maybeBibData = Some(bibRecord))

  def apply(id: String, itemRecord: SierraItemRecord): SierraTransformable =
    SierraTransformable(id = id, itemData = Map(itemRecord.id -> itemRecord))

  def apply(bibRecord: SierraBibRecord, version: Int): SierraTransformable =
    SierraTransformable(
      id = bibRecord.id,
      maybeBibData = Some(bibRecord),
      version = version
    )
}
