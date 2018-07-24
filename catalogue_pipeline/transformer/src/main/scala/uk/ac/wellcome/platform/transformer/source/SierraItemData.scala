package uk.ac.wellcome.platform.transformer.source

import cats.syntax.either._
import io.circe.Decoder
import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber

case class SierraItemData(
  id: SierraRecordNumber,
  deleted: Boolean = false,
  location: Option[SierraItemLocation] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)

case class SierraItemLocation(code: String, name: String)

/** A Circe decoder that allows unpacking the ID as an instance of
  * [[SierraRecordNumber]].  This is based the example Instant decoder
  * described in the Circe docs.
  * See: https://circe.github.io/circe/codecs/custom-codecs.html
  *
  * To use this implicit, add the following import to a file:
  *
  *     import uk.ac.wellcome.platform.transformer.source.SierraItemData._
  *
  */
case object SierraItemData {
  implicit val decodeSierraRecordNumber: Decoder[SierraRecordNumber] =
    Decoder.decodeString.emap { str =>
      Either.catchNonFatal(SierraRecordNumber(str)).leftMap(t => "SierraRecordNumber")
    }
}
