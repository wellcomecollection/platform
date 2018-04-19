package uk.ac.wellcome.message

import java.net.URI
import io.circe._
import cats.syntax.either._

case class MessagePointer(src: URI)

object MessagePointer {

  def apply(uri: String): MessagePointer = MessagePointer(new URI(uri))

  implicit val encoder = Encoder.instance[MessagePointer] { pointer =>
    Json.obj(("src", Json.fromString(pointer.src.toString)))
  }

  implicit val decoder = Decoder.instance[MessagePointer] { cursor =>
    for {
      src <- cursor.downField("src").as[String]
    } yield {
      MessagePointer(new URI(src))
    }
  }

}
