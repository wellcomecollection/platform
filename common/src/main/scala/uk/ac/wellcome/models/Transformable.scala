package uk.ac.wellcome.models

import uk.ac.wellcome.utils.JsonUtil
import scala.util.Try

trait Transformable {
  def transform: Try[Work]
}

case class MiroTransformableData(image_title: Option[String])

case class MiroTransformable(MiroID: String,
                             MiroCollection: String,
                             data: String)
    extends Transformable {
  override def transform: Try[Work] =
    JsonUtil.fromJson[MiroTransformableData](data).map { miroData =>
      Work(identifiers =
                    List(SourceIdentifier("Miro", "MiroID", MiroID)),
                  label = miroData.image_title.getOrElse("no label found"))
    }
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
