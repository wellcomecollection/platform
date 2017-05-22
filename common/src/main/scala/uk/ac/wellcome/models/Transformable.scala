package uk.ac.wellcome.models

import uk.ac.wellcome.utils.JsonUtil

import scala.util.Try

trait Transformable {
  def transform: Try[Work]
  val reindexVersion: Option[Int] = None
}

case class CalmTransformableData(
  AccessStatus: Array[String]
) extends Transformable {
  def transform: Try[Work] = Try {
    Work(
      identifiers = List(SourceIdentifier("source", "key", "value")),
      label = "calm data label"
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
      .fromJson[CalmTransformableData](data)
      .flatMap(_.transform)

}
