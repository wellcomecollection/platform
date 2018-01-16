package uk.ac.wellcome.circe

import java.time.Instant

import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.generic.extras.{AutoDerivation, Configuration}
import io.circe.parser.decode
import io.circe.syntax._
import cats.syntax.either._

import scala.util.Try

object jsonUtil extends AutoDerivation {

  import uk.ac.wellcome.models.transformable.Transformable._

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
    Configuration.default.withDefaults.withDiscriminator("type").copy(transformMemberNames = {
      case "ontologyType" => "type"
      case other => other
    })

  def toJsonCirce[T](value: T)(implicit encoder: Encoder[T]): Try[String] = {
    Try(value.asJson.noSpaces)
  }

  def fromJsonCirce[T](json:String)(implicit decoder: Decoder[T]): Try[T] = {
    decode[T](json).toTry
  }
}
