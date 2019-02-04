package uk.ac.wellcome.platform.archive.common.models.bagit

import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.TypeCoercionError
import io.circe.{Decoder, Encoder, Json}
import uk.ac.wellcome.json.JsonUtil.{fromJson, toJson}

case class BagItemPath(underlying: String) extends AnyVal {
  override def toString: String = underlying
}

object BagItemPath {
  implicit val encoder: Encoder[BagItemPath] = Encoder.instance[BagItemPath] {
    space: BagItemPath =>
      Json.fromString(space.toString)
  }

  implicit val decoder: Decoder[BagItemPath] =
    Decoder.instance[BagItemPath](cursor =>
      cursor.value.as[String].map(BagItemPath(_)))

  implicit def fmtSpace: DynamoFormat[BagItemPath] =
    DynamoFormat.xmap[BagItemPath, String](
      fromJson[BagItemPath](_)(decoder).toEither.left
        .map(TypeCoercionError)
    )(
      toJson[BagItemPath](_).get
    )
}
