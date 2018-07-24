package uk.ac.wellcome.models.transformable

import io.circe._
import uk.ac.wellcome.models.Sourced
import uk.ac.wellcome.models.transformable.sierra.{
  SierraBibRecord,
  SierraItemRecord,
  SierraRecordNumber
}

sealed trait Transformable extends Sourced

case class MiroTransformable(sourceId: String,
                             sourceName: String = "miro",
                             MiroCollection: String,
                             data: String)
    extends Transformable

case class SierraTransformable(
  sierraId: SierraRecordNumber,
  sourceName: String = "sierra",
  maybeBibRecord: Option[SierraBibRecord] = None,
  itemRecords: Map[SierraRecordNumber, SierraItemRecord] = Map()
) extends Transformable {
  override val sourceId: String = sierraId.withoutCheckDigit
}

object SierraTransformable {
  def apply(bibRecord: SierraBibRecord): SierraTransformable =
    SierraTransformable(
      sierraId = bibRecord.id,
      maybeBibRecord = Some(bibRecord)
    )

  // Because the [[SierraTransformable.itemRecords]] field is keyed by
  // [[SierraRecordNumber]] in our case class, but JSON only supports string
  // keys, we need to turn the ID into a string when storing as JSON.
  //
  // This is based on the "Custom key types" section of the Circe docs:
  // https://circe.github.io/circe/codecs/custom-codecs.html#custom-key-types
  //
  implicit val keyEncoder: KeyEncoder[SierraRecordNumber] =
    (key: SierraRecordNumber) => key.withoutCheckDigit

  implicit val keyDecoder: KeyDecoder[SierraRecordNumber] = (key: String) =>
    Some(SierraRecordNumber(key))
}
