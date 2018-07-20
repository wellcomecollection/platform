package uk.ac.wellcome.models.transformable

import io.circe._
import io.circe.syntax._
import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord,
  SierraRecordNumber
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
  */
case class SierraTransformable(
  sierraId: SierraRecordNumber,
  sourceName: String = "sierra",
  maybeBibRecord: Option[SierraBibRecord] = None,
  itemRecords: Map[SierraRecordNumber, SierraItemRecord] = Map()
) extends Transformable {
  def sourceId: String =
    sierraId.withoutCheckDigit
}

object SierraTransformable {
  def apply(bibRecord: SierraBibRecord): SierraTransformable =
    SierraTransformable(
      sierraId = bibRecord.id,
      maybeBibRecord = Some(bibRecord)
    )
}

/** Provides a Circe encoder/decoder for SierraTransformable.
  *
  * Because the [[SierraTransformable.itemRecords]] field is keyed by
  * [[SierraRecordNumber]] in our case class, but JSON only supports string
  * keys, we need to turn the ID into a string when storing as JSON.
  *
  * To use these helpers, add
  *
  *     import uk.ac.wellcome.models.transformable.SierraTransformableCodec._
  *
  * to your imports.
  *
  */
object SierraTransformableCodec {
  implicit val itemRecordsDecoder
    : Decoder[Map[SierraRecordNumber, SierraItemRecord]] =
    Decoder.instance[Map[SierraRecordNumber, SierraItemRecord]] {
      cursor: HCursor =>
        cursor
          .as[Map[String, SierraItemRecord]]
          .map { itemRecordsByString: Map[String, SierraItemRecord] =>
            itemRecordsByString
              .map {
                case (id: String, itemRecord: SierraItemRecord) =>
                  SierraRecordNumber(id) -> itemRecord
              }
          }
    }

  implicit val itemRecordsEncoder
    : Encoder[Map[SierraRecordNumber, SierraItemRecord]] =
    Encoder.instance[Map[SierraRecordNumber, SierraItemRecord]] {
      itemRecords: Map[SierraRecordNumber, SierraItemRecord] =>
        Json.fromFields(
          itemRecords.map {
            case (id: SierraRecordNumber, itemRecord: SierraItemRecord) =>
              id.withoutCheckDigit -> itemRecord.asJson
          }
        )
    }
}
