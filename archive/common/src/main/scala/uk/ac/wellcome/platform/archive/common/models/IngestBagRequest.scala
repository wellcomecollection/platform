package uk.ac.wellcome.platform.archive.common.models

import java.net.{URI, URISyntaxException}
import java.util.UUID

import uk.ac.wellcome.json.JsonUtil._

import io.circe.DecodingFailure
import uk.ac.wellcome.storage.ObjectLocation

case class IngestBagRequest(archiveRequestId: UUID,
                            zippedBagLocation: ObjectLocation,
                            archiveCompleteCallbackUrl: Option[URI] = None)

object IngestBagRequest {
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

  implicit val ingestBagRequestNotificationDecoder: Decoder[IngestBagRequest] =
    deriveDecoder
  implicit val ingestBagRequestNotificationEncoder: Encoder[IngestBagRequest] =
    deriveEncoder
}
