package uk.ac.wellcome.platform.archive.archivist.models

import java.net.{URI, URISyntaxException}
import java.util.UUID

import io.circe.DecodingFailure
import uk.ac.wellcome.storage.ObjectLocation
import uk.ac.wellcome.json.JsonUtil._

case class IngestBagRequestNotification(
  archiveRequestId: UUID,
  bagLocation: ObjectLocation,
  archiveCompleteCallbackUrl: Option[URI] = None)

object IngestBagRequestNotification {
  import io.circe.generic.semiauto._
  import io.circe.{Decoder, Encoder}

  implicit val uriEncoder: Encoder[URI] =
    Encoder.encodeString.contramap[URI](_.toString)
  implicit val uriDecoder: Decoder[URI] = Decoder.instance { cursor =>
    cursor.as[String] match {
      case Right(str) =>
        try Right(new URI(str))
        catch {
          case _: URISyntaxException =>
            Left(DecodingFailure("URI", cursor.history))
        }
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[URI]]
    }
  }

  implicit val uuidEncoder: Encoder[UUID] =
    Encoder.encodeString.contramap[UUID](_.toString)
  implicit val uuidDecoder: Decoder[UUID] = Decoder.instance { cursor =>
    cursor.as[String] match {
      case Right(str) =>
        try Right(UUID.fromString(str))
        catch {
          case _: IllegalArgumentException =>
            Left(DecodingFailure("UUID", cursor.history))
        }
      case l @ Left(_) => l.asInstanceOf[Decoder.Result[UUID]]
    }
  }

  implicit val ingestBagRequestNotificationDecoder
    : Decoder[IngestBagRequestNotification] = deriveDecoder
  implicit val ingestBagRequestNotificationEncoder
    : Encoder[IngestBagRequestNotification] = deriveEncoder
}
