package uk.ac.wellcome.platform.archive.common.models

import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.TypeCoercionError
import io.circe.{Decoder, Encoder, Json}
import uk.ac.wellcome.json.JsonUtil.{fromJson, toJson}

case class StorageSpace(underlying: String) extends AnyVal {
  override def toString: String = underlying
}

object StorageSpace {
  implicit val spaceEnc = Encoder.instance[StorageSpace] {
    space: StorageSpace =>
      Json.fromString(space.toString)
  }

  implicit val spaceDec = Decoder.instance[StorageSpace](cursor =>
    cursor.value.as[String].map(StorageSpace(_)))

  implicit def fmtSpace =
    DynamoFormat.xmap[StorageSpace, String](
      fromJson[StorageSpace](_)(spaceDec).toEither.left
        .map(TypeCoercionError)
    )(
      toJson[StorageSpace](_).get
    )
}
