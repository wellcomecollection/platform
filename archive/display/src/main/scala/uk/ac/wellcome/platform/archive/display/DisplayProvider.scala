package uk.ac.wellcome.platform.archive.display

import io.circe.CursorOp.DownField
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import uk.ac.wellcome.platform.archive.common.progress.models.{
  InfrequentAccessStorageProvider,
  StandardStorageProvider,
  StorageProvider
}

sealed trait DisplayProvider {
  val id: String
  def toStorageProvider: StorageProvider
}

case object StandardDisplayProvider extends DisplayProvider {
  override val id: String = "aws-s3-standard"

  override def toStorageProvider: StorageProvider = StandardStorageProvider
}

case object InfrequentAccessDisplayProvider extends DisplayProvider {
  override val id: String = "aws-s3-ia"

  override def toStorageProvider: StorageProvider =
    InfrequentAccessStorageProvider
}

object DisplayProvider {
  def apply(provider: StorageProvider): DisplayProvider =
    provider match {
      case StandardStorageProvider         => StandardDisplayProvider
      case InfrequentAccessStorageProvider => InfrequentAccessDisplayProvider
    }

  implicit val decoder
    : Decoder[DisplayProvider] = Decoder.instance[DisplayProvider](cursor =>
    for {
      id <- cursor.downField("id").as[String]
      provider <- id match {
        case StandardDisplayProvider.id => Right(StandardDisplayProvider)
        case InfrequentAccessDisplayProvider.id =>
          Right(InfrequentAccessDisplayProvider)
        case invalidId =>
          val fields = DownField("id") +: cursor.history
          Left(DecodingFailure(
            s"""got "$invalidId", valid values are: ${StandardDisplayProvider.id}, ${InfrequentAccessDisplayProvider.id}.""",
            fields))
      }
    } yield {
      provider
  })

  implicit val encoder: Encoder[DisplayProvider] =
    Encoder.instance[DisplayProvider] { provider =>
      Json.obj(
        "id" -> Json.fromString(provider.id),
        "type" -> Json.fromString("Provider"))
    }
}
