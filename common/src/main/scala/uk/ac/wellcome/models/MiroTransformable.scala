package uk.ac.wellcome.models

import scala.util.Try

import com.fasterxml.jackson.annotation.JsonProperty

import uk.ac.wellcome.utils.JsonUtil

case class MiroTransformableData(
  @JsonProperty("image_title") title: Option[String],
  @JsonProperty("image_creator") creator: Option[List[String]]
)

case class MiroTransformable(MiroID: String,
                             MiroCollection: String,
                             data: String)
    extends Transformable {
  override def transform: Try[Work] =
    JsonUtil.fromJson[MiroTransformableData](data).map { miroData =>

      // Identifier is passed straight through
      val identifiers = List(SourceIdentifier("Miro", "MiroID", MiroID))

      // In the Miro XML dumps, <image_title> is the Short Description.
      // This maps to our data property "label".
      val label = miroData.title.get

      // In the Miro XML dumps, <image_creator> is the Creator, which
      // maps to our object property "hasCreator"
      val creators: List[Agent] = miroData.creator match {
        case Some(c) => c.map { Agent(_) }
        case None => List()
      }

      Work(
        identifiers = identifiers,
        label = label,
        hasCreator = creators
      )
    }
}
