package uk.ac.wellcome.utils

import cats.syntax.either._
import com.twitter.inject.Logging
import io.circe.generic.extras.{AutoDerivation, Configuration}
import io.circe.java8.time.TimeInstances
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}
import uk.ac.wellcome.exceptions.GracefulFailureException

import scala.util.Try

object JsonUtil extends AutoDerivation with TimeInstances with Logging {

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
