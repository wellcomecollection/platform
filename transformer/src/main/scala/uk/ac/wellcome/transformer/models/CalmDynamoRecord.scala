package uk.ac.wellcome.platform.transformer.models

import uk.ac.wellcome.utils.JsonUtil
import scala.util.Try

trait Transformable {
  def transform: Try[CleanedRecord]
}

case class CleanedRecord(
  source: String,
  accessStatus: Option[String]
)

case class DirtyCalmRecord(
  AccessStatus: Option[String]
) extends Transformable {
  def transform: Try[CleanedRecord] = Try {
    CleanedRecord(
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
