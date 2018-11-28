package uk.ac.wellcome.platform.archive.common.models

import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.TypeCoercionError
import io.circe.{Decoder, Encoder, Json}
import uk.ac.wellcome.json.JsonUtil._

case class BagId(
  space: Namespace,
  externalIdentifier: ExternalIdentifier
) {
  override def toString: String =
    s"$space/$externalIdentifier"

}

case class ExternalIdentifier(underlying: String) extends AnyVal {
  override def toString: String = underlying
}

object ExternalIdentifier {
  implicit val spaceEnc = Encoder.instance[ExternalIdentifier] {
    space: ExternalIdentifier =>
      Json.fromString(space.toString)
  }

  implicit val spaceDec = Decoder.instance[ExternalIdentifier](cursor =>
    cursor.value.as[String].map(ExternalIdentifier(_)))

  implicit def fmtSpace =
    DynamoFormat.xmap[ExternalIdentifier, String](
      fromJson[ExternalIdentifier](_)(spaceDec).toEither.left
        .map(TypeCoercionError)
    )(
      toJson[ExternalIdentifier](_).get
    )
}
