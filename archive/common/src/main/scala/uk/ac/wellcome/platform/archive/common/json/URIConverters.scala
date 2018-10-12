package uk.ac.wellcome.platform.archive.common.json

import java.net.{URI, URISyntaxException}

import com.gu.scanamo.DynamoFormat
import io.circe.{Decoder, DecodingFailure, Encoder}
import uk.ac.wellcome.json.JsonUtil.{fromJson, toJson}

trait URIConverters {
  implicit val fmtUri =
    DynamoFormat.coercedXmap[URI, String, IllegalArgumentException](
      fromJson[URI](_).get
    )(
      toJson[URI](_).get
    )

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
}
