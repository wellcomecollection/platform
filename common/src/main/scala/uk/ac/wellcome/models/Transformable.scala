package uk.ac.wellcome.models

import scala.util.Try

import com.fasterxml.jackson.annotation.JsonProperty

import uk.ac.wellcome.utils.JsonUtil

trait Transformable {
  def transform: Try[Work]
}

case class MiroTransformableData(
  @JsonProperty("image_title") title: Option[String],
  @JsonProperty("image_image_desc") imageDesc: Option[String],
  @JsonProperty("image_supp_lettering") suppLettering: Option[String]
)

case class MiroTransformable(MiroID: String,
                             MiroCollection: String,
                             data: String)
    extends Transformable {
  override def transform: Try[Work] =
    JsonUtil.fromJson[MiroTransformableData](data).map { miroData =>
      MiroCollection match {
        case "Images-A" => transformImagesA(miroData)
        case _ => throw new Exception(s"Unable to transform unknown collection $MiroCollection")
      }
    }

  // The mapping of Miro fields to UnifiedItem fields is quite rough,
  // and based on a cursory expection of the Miro data.
  // TODO: Get a proper mapping of fields.

  private def transformImagesA(miroData: MiroTransformableData): Work =
    Work(
      identifiers = List(SourceIdentifier("Miro", "MiroID", MiroID)),
      label = miroData.title.getOrElse("no label found"),
      description = miroData.imageDesc,
      lettering = miroData.suppLettering,
      hasCreatedDate = None,
      hasCreator = List()
    )
}

case class CalmDataTransformable(
  AccessStatus: Array[String]
) extends Transformable {
  def transform: Try[Work] = Try {
    Work(
      identifiers = List(SourceIdentifier("source", "key", "value")),
      label = "calm data label",
      accessStatus = AccessStatus.headOption
    )
  }
}

//TODO add some tests around transformation
case class CalmTransformable(
  RecordID: String,
  RecordType: String,
  AltRefNo: String,
  RefNo: String,
  data: String
) extends Transformable {

  def transform: Try[Work] =
    JsonUtil
      .fromJson[CalmDataTransformable](data)
      .flatMap(_.transform)

}
