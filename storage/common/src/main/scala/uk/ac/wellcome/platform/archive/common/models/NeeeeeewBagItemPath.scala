package uk.ac.wellcome.platform.archive.common.models

import com.gu.scanamo.DynamoFormat
import com.gu.scanamo.error.TypeCoercionError
import io.circe.{Decoder, Encoder, Json}
import uk.ac.wellcome.json.JsonUtil.{fromJson, toJson}
import uk.ac.wellcome.storage.ObjectLocation

case class NeeeeeewBagItemPath(underlying: String) extends AnyVal {
  override def toString: String = underlying
}

object NeeeeeewBagItemPath {
  implicit val encoder: Encoder[NeeeeeewBagItemPath] = Encoder.instance[NeeeeeewBagItemPath] {
    space: NeeeeeewBagItemPath =>
      Json.fromString(space.toString)
  }

  implicit val decoder: Decoder[NeeeeeewBagItemPath] = Decoder.instance[NeeeeeewBagItemPath](cursor =>
    cursor.value.as[String].map(NeeeeeewBagItemPath(_)))

  implicit def fmtSpace: DynamoFormat[NeeeeeewBagItemPath] =
    DynamoFormat.xmap[NeeeeeewBagItemPath, String](
      fromJson[NeeeeeewBagItemPath](_)(decoder).toEither.left
        .map(TypeCoercionError)
    )(
      toJson[NeeeeeewBagItemPath](_).get
    )
}

case class NeeeeeeewBagItemLocation(
  bagLocation: BagLocation,
  bagItemPath: NeeeeeewBagItemPath
) {
  def completePath: String =
    List(bagLocation.completeFilepath, bagItemPath).mkString("/")

  def objectLocation: ObjectLocation =
    ObjectLocation(
      namespace = bagLocation.storageNamespace,
      key = completePath
    )
}
