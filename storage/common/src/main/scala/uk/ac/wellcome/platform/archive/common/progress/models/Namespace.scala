package uk.ac.wellcome.platform.archive.common.progress.models

import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.TypeCoercionError
import io.circe.{Decoder, Encoder, Json}
import uk.ac.wellcome.json.JsonUtil.{fromJson, toJson}

case class Namespace(underlying: String) extends AnyVal {
  override def toString: String = underlying
}

object Namespace {
  implicit val namespaceEnc =
    Encoder.instance[Namespace] { space: Namespace =>
      Json.fromString(space.toString)
    }

  implicit val namespaceDec = Decoder.instance[Namespace](cursor =>
    cursor.value.as[String].map(Namespace(_)))

  implicit def fmtNamespace =
    DynamoFormat.xmap[Namespace, String](
      fromJson[Namespace](_)(namespaceDec).toEither.left
        .map(TypeCoercionError)
    )(
      toJson[Namespace](_).get
    )
}
