package uk.ac.wellcome.utils

import java.net.{URI, URISyntaxException}
import java.time.Instant
import java.time.format.DateTimeParseException

import cats.syntax.either._
import com.twitter.inject.Logging
import io.circe.generic.extras.{AutoDerivation, Configuration}
import io.circe.java8.time.TimeInstances
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import uk.ac.wellcome.exceptions.GracefulFailureException

import scala.util.Try

trait URIInstances {
   implicit final val decodeURI: Decoder[URI] =
    Decoder.instance { c =>
      c.as[String] match {
        case Right(s) => try Right(new URI(s)) catch {
          case _: URISyntaxException=> Left(DecodingFailure("URI", c.history))
        }
        case l @ Left(_) => l.asInstanceOf[Decoder.Result[URI]]
      }
    }

  implicit final val encodeURI: Encoder[URI] = Encoder.instance(uri => Json.fromString(uri.toString))
}

object JsonUtil extends AutoDerivation with TimeInstances with URIInstances with Logging {


  implicit val customConfig: Configuration =
    Configuration.default.withDefaults
      .withDiscriminator("type")

  def toJson[T](value: T)(implicit encoder: Encoder[T]): Try[String] =
    Try(value.asJson.noSpaces)

  def toMap[T](json: String)(
    implicit decoder: Decoder[T]): Try[Map[String, T]] =
    fromJson[Map[String, T]](json)

  def fromJson[T](json: String)(implicit decoder: Decoder[T]): Try[T] =
    decode[T](json).toTry.recover {
      case e: Exception =>
        warn(s"Error when trying to decode $json", e)
        throw GracefulFailureException(e)
    }
}
