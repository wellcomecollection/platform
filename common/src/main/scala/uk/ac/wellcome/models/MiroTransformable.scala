package uk.ac.wellcome.models

import scala.util.Try

import com.fasterxml.jackson.annotation.JsonProperty

import uk.ac.wellcome.utils.JsonUtil

case class MiroTransformableData(
  @JsonProperty("image_title") title: Option[String],
  @JsonProperty("image_creator") creator: Option[List[String]],
  @JsonProperty("image_image_desc") description: Option[String],
  @JsonProperty("image_secondary_creator") secondaryCreator: Option[List[String]],
  @JsonProperty("image_artwork_date") artworkDate: Option[String]
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

      // <image_secondary_creator>: what MIRO calls Secondary Creator, which
      // will also just have to map to our object property "hasCreator"
      val secondaryCreators: List[Agent] = miroData.secondaryCreator match {
        case Some(c) => c.map { Agent(_) }
        case None => List()
      }

      // Determining the creation date depends on several factors, so we do
      // it on a per-collection basis.
      val createdDate: Option[Period] = MiroCollection match {
        case "Images_V" => miroData.artworkDate.map { Period(_) }
        case _ => None
      }

      Work(
        identifiers = identifiers,
        label = label,
        description = description,
        hasCreatedDate = createdDate,
        hasCreator = creators ++ secondaryCreators
      )
    }
}
