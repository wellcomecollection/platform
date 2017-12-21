package uk.ac.wellcome

import java.time.Instant

import io.circe.{Decoder, HCursor}
import cats.syntax.either._
import io.circe.generic.extras.Configuration

package object circe {
  implicit val decodeInstant: Decoder[Instant] = new Decoder[Instant] {
    final def apply(c: HCursor): Decoder.Result[Instant] =
      for {
        epochSeconds <- c.as[Long]
      } yield {
        Instant.ofEpochSecond(epochSeconds)
      }
  }

  implicit val customConfig: Configuration =
    Configuration.default.withDefaults //.withDiscriminator("type")
}
