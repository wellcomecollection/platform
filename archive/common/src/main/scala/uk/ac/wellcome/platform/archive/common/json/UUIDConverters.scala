package uk.ac.wellcome.platform.archive.common.json

import java.util.UUID

import com.gu.scanamo.DynamoFormat
import io.circe.{Decoder, DecodingFailure, Encoder}
import uk.ac.wellcome.json.JsonUtil.{fromJson, toJson}

trait UUIDConverters {
  implicit val fmtUuid =
    DynamoFormat.coercedXmap[UUID, String, IllegalArgumentException](
      fromJson[UUID](_).get
    )(
      toJson[UUID](_).get
    )

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
}
