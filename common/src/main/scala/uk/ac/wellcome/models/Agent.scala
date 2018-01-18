package uk.ac.wellcome.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.{Decoder, Encoder, HCursor, Json}
import cats.syntax.either._

class Agent(
  val label: String,
  @JsonProperty("type") val ontologyType: String = "Agent"
)

object Agent {

  def apply(label: String): Agent = new Agent(label)
  Encoder
  implicit val encoder= Encoder.instance[Agent]{a: Agent=> {
      Json.obj(
        ("label", Json.fromString(a.label)),
        ("type", Json.fromString(a.ontologyType))
      )
    }
  }
  implicit val decoder = Decoder.instance[Agent]{ cursor: HCursor =>
    for {
      label <- cursor.downField("label").as[String]
    } yield {
      new Agent(label)
    }
  }
}
