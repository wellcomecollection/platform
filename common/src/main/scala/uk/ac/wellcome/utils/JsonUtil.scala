package uk.ac.wellcome.utils

import java.time.Instant

import cats.syntax.either._
import com.twitter.inject.Logging
import io.circe.generic.extras.{AutoDerivation, Configuration}
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder, HCursor, Json}
import uk.ac.wellcome.exceptions.GracefulFailureException

import scala.util.Try

object JsonUtil extends AutoDerivation with Logging {

  implicit val decodeInstant: Decoder[Instant] = new Decoder[Instant] {
    final def apply(c: HCursor): Decoder.Result[Instant] = {
      List(
        c.as[Long],
        c.as[Double].map(_.toLong)
      ).filter(_.isRight)
        .head
        .map(i => Instant.ofEpochSecond(i))
    }
  }

  implicit val encodeInstant: Encoder[Instant] = new Encoder[Instant] {
    override def apply(value: Instant): Json =
      Json.fromInt(value.getEpochSecond.toInt)
  }

  implicit val customConfig: Configuration =
    Configuration.default.withDefaults
      .withDiscriminator("type")
      .copy(transformMemberNames = {
        case "ontologyType" => "type"
        case other => other
      })

  def toJson[T](value: T)(implicit encoder: Encoder[T]): Try[String] = Try(value.asJson.noSpaces)

  def toMap[T](json: String)(
    implicit decoder: Decoder[T]): Try[Map[String, T]] =
    fromJson[Map[String, T]](json)

  def fromJson[T](json: String)(implicit decoder: Decoder[T]): Try[T] = decode[T](json).toTry.recover {
    case e: Exception =>
      warn("Invalid message structure (not via SNS?)", e)
      throw GracefulFailureException(e)
  }
}
