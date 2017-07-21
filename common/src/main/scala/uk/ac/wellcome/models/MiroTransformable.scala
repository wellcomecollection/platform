package uk.ac.wellcome.models

import scala.util.Try

import com.fasterxml.jackson.annotation.JsonProperty

import uk.ac.wellcome.utils.JsonUtil

case class MiroTransformableData(
  @JsonProperty("image_title") title: Option[String],
  @JsonProperty("image_creator") creator: Option[List[String]],
  @JsonProperty("image_image_desc") description: Option[String],
  @JsonProperty("image_secondary_creator") secondaryCreator: Option[
    List[String]],
  @JsonProperty("image_artwork_date") artworkDate: Option[String],
  @JsonProperty("image_cleared") cleared: Option[String],
  @JsonProperty("image_copyright_cleared") copyright_cleared: Option[String]
)

case class ShouldNotTransformException(message: String)
  extends Exception(message)

case class MiroTransformable(MiroID: String,
                             MiroCollection: String,
                             data: String,
                             ReindexShard: String = "default",
                             ReindexVersion: Int = 0)
    extends Transformable
    with Reindexable[String] {

  val id: ItemIdentifier[String] = ItemIdentifier(
    HashKey("MiroID", MiroID),
    RangeKey("MiroCollection", MiroCollection)
  )

  override def transform: Try[Work] =
    JsonUtil.fromJson[MiroTransformableData](data).map { miroData =>
      // Identifier is passed straight through
      val identifiers = List(SourceIdentifier("Miro", "MiroID", MiroID))

      // XML tags refer to fields within the Miro XML dumps.

      // If the <image_cleared> or <image_copyright_cleared> fields are
      // missing or don't have have the value 'Y', then we shouldn't expose
      // this image in the public API.
      // See https://github.com/wellcometrust/platform-api/issues/356
      if (miroData.cleared.getOrElse("N") != "Y") {
        throw new ShouldNotTransformException("image_cleared field is not Y")
      }
      if (miroData.copyright_cleared.getOrElse("N") != "Y") {
        throw new ShouldNotTransformException(
          "image_copyright_cleared field is not Y")
      }

      //TODO: Ensure we throw ShouldNotTransformException in the correct places

      // In at least the V collection, many of the titles are truncated
      // versions of the description.  In this case, we drop the description
      // field and put the full description in the label instead.
      val titleIsTruncatedDescription = miroData.description
        .getOrElse("")
        .startsWith(miroData.title.get)

      val useDescriptionAsTitle = (titleIsTruncatedDescription &&
        MiroCollection == "Images-V")

      // <image_title>: the Short Description.  This maps to our property
      // "label".
      //
      // Every image in the V collection that has image_cleared == Y has a
      // non-empty title.  This is _not_ true for the MIRO records in general.
      // TODO: Work out what label to use for those records.
      val label =
        if (useDescriptionAsTitle) miroData.description.get
        else miroData.title.get

      // <image_image_desc>: the Description, which maps to our property
      // "description".
      val description =
        if (useDescriptionAsTitle) None else miroData.description

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
        case "Images-V" => miroData.artworkDate.map { Period(_) }
        case _ => None
      }

      Work(
        identifiers = identifiers,
        label = label,
        description = description,
        createdDate = createdDate,
        creators = creators ++ secondaryCreators
      )
    }
}
