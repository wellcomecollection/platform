package uk.ac.wellcome

import java.time.Instant

import io.circe.{Decoder, HCursor}
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

  implicit val customConfig: Configuration =
    Configuration.default.withDefaults.withDiscriminator("type")
}
