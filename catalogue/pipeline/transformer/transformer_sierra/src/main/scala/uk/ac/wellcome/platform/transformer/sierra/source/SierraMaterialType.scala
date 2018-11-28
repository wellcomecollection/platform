package uk.ac.wellcome.platform.transformer.sierra.source

import io.circe.Decoder

case class SierraMaterialType(code: String)

object SierraMaterialType {
  implicit val decoder = Decoder.instance[SierraMaterialType](cursor =>
    for {
      id <- cursor.downField("code").as[String]
    } yield {
      SierraMaterialType(id.trim)
  })
}
