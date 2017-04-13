package uk.ac.wellcome.models

import uk.ac.wellcome.utils.JsonUtil
import scala.util.Try

trait Transformable {
  def transform: Try[UnifiedItem]
}

case class MiroTransformableData(image_title: Option[String])

case class MiroTransformable(MiroID: String, MiroCollection: String, data: String) extends Transformable {
  override def transform: Try[UnifiedItem] =
    JsonUtil.fromJson[MiroTransformableData](data).map { miroData =>
      UnifiedItem(List(SourceIdentifier("Miro", "MiroID", MiroID)), miroData.image_title, None)
    }
}

case class CalmDataTransformable(
  AccessStatus: Array[String]
) extends Transformable {
  def transform: Try[UnifiedItem] = Try {
    UnifiedItem(
      List(SourceIdentifier("source", "key", "value")),
      None,
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

  def transform: Try[UnifiedItem] =
    JsonUtil
      .fromJson[CalmDataTransformable](data)
      .flatMap(_.transform)

}

