package uk.ac.wellcome.models

import uk.ac.wellcome.utils.JsonUtil
import uk.ac.wellcome.models.UnifiedItem
import scala.util.Try

trait Transformable {
  def transform: Try[CleanedRecord]
}

case class DirtyCalmRecord(
  AccessStatus: Option[String]
) extends Transformable {
  def transform: Try[UnifiedItem] = Try {
    UnifiedItem(
      "Foo",
      accessStatus = AccessStatus
    )
  }
}

case class CalmDynamoRecord(
  RecordID: String,
  RecordType: String,
  AltRefNo: String,
  RefNo: String,
  data: String
) extends Transformable {

  def transform: Try[CleanedRecord] =
    JsonUtil
      .fromJson[DirtyCalmRecord](data)
      .flatMap(_.transform)

}
