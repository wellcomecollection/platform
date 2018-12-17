package uk.ac.wellcome.platform.archive.display

import io.circe.CursorOp.DownField
import io.circe.{Decoder, DecodingFailure, Encoder, Json}

sealed trait DisplayIngestType{val id: String}

object CreateDisplayIngestType extends DisplayIngestType {
  override val id: String = "create"
}

object DisplayIngestType {

  implicit val decoder: Decoder[DisplayIngestType] = Decoder.instance[DisplayIngestType](cursor =>
    for {
      id <- cursor.downField("id").as[String]
      ingestType <- id match {
        case CreateDisplayIngestType.id => Right(CreateDisplayIngestType)
        case _ =>
          val fields = DownField("id") +: cursor.history
          Left(DecodingFailure(s"invalid value supplied, valid values are: ${CreateDisplayIngestType.id}.", fields))
      }
    } yield {
      ingestType
    })

  implicit val encoder: Encoder[DisplayIngestType] = Encoder.instance[DisplayIngestType] { ingestType =>
    Json.obj("id" -> Json.fromString(ingestType.id), "type" -> Json.fromString("IngestType"))
  }
}