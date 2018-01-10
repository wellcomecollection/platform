package uk.ac.wellcome

import java.time.Instant

import io.circe.{Decoder, Encoder, HCursor, Json}
import cats.syntax.either._
import io.circe.generic.extras.Configuration

package object circe {
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
    Configuration.default.withDefaults.withDiscriminator("type")
}
