package uk.ac.wellcome.platform.transformer.source

import cats.syntax.either._
import io.circe.Decoder
import uk.ac.wellcome.models.transformable.sierra.SierraRecordNumber
import uk.ac.wellcome.platform.transformer.source.sierra.{
  Country => SierraCountry,
  Language => SierraLanguage
}

case class SierraBibData(
  id: SierraRecordNumber,
  title: Option[String] = None,
  deleted: Boolean = false,
  suppressed: Boolean = false,
  country: Option[SierraCountry] = None,
  lang: Option[SierraLanguage] = None,
  materialType: Option[SierraMaterialType] = None,
  fixedFields: Map[String, FixedField] = Map(),
  varFields: List[VarField] = List()
)

/** A Circe decoder that allows unpacking the ID as an instance of
  * [[SierraRecordNumber]].  This is based on the example Instant decoder
  * described in the Circe docs.
  * See: https://circe.github.io/circe/codecs/custom-codecs.html
  */
case object SierraBibData {
  implicit val sierraRecordNumberDecoder: Decoder[SierraRecordNumber] =
    Decoder.decodeString.emap { str =>
      Either
        .catchNonFatal(SierraRecordNumber(str))
        .leftMap(t => "SierraRecordNumber")
    }
}
