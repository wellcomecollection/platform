package uk.ac.wellcome.models

import uk.ac.wellcome.utils.JsonUtil
import scala.util.Try

trait Transformable {
  def transform: Try[UnifiedItem]
}

case class MiroTransformableData(image_title: Option[String])

case class MiroTransformable(MiroID: String,
                             MiroCollection: String,
                             data: String)
    extends Transformable {
  override def transform: Try[UnifiedItem] =
    JsonUtil.fromJson[MiroTransformableData](data).map { miroData =>
      UnifiedItem(identifiers =
                    List(SourceIdentifier("Miro", "MiroID", MiroID)),
                  title = miroData.image_title)
    }
}

case class CalmDataTransformable(
  AccessStatus: Array[String]
) extends Transformable {
  def transform: Try[UnifiedItem] = Try {
    UnifiedItem(
      identifiers = List(SourceIdentifier("source", "key", "value")),
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
