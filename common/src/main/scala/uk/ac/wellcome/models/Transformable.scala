package uk.ac.wellcome.models

import uk.ac.wellcome.utils.JsonUtil
import scala.util.Try

trait Transformable {
  def transform: Try[UnifiedItem]
}

case class CalmDataTransformable(
  AccessStatus: Array[String]
) extends Transformable {
  def transform: Try[UnifiedItem] = Try {
    UnifiedItem(
      List(SourceIdentifier("source", "key", "value")),
      accessStatus = AccessStatus.headOption
    )
  }
}

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
