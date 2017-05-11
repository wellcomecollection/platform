package uk.ac.wellcome.models

import scala.util.Try

import com.fasterxml.jackson.annotation.JsonProperty

import uk.ac.wellcome.utils.JsonUtil

case class MiroTransformableData(
  @JsonProperty("image_title") title: Option[String],
  @JsonProperty("image_creator") creator: Option[List[String]],
  @JsonProperty("image_image_desc") description: Option[String]
)

case class MiroTransformable(MiroID: String,
                             MiroCollection: String,
                             data: String)
    extends Transformable {
  override def transform: Try[Work] =
    JsonUtil.fromJson[MiroTransformableData](data).map { miroData =>

      // Identifier is passed straight through
      val identifiers = List(SourceIdentifier("Miro", "MiroID", MiroID))

      // XML tags refer to fields within the Miro XML dumps.

      // <image_title>: the Short Description.  This maps to our property
      // "label".
      val label = miroData.title.get

      // <image_image_desc>: the Description, which maps to our property
      // "description".
      val description = miroData.description

      // <image_creator>: the Creator, which maps to our property "hasCreator"
      val creators: List[Agent] = miroData.creator match {
        case Some(c) => c.map { Agent(_) }
        case None => List()
      }

      Work(
        identifiers = identifiers,
        label = label,
        description = description,
        hasCreator = creators
      )
    }
}
