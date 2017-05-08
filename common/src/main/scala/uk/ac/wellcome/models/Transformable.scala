package uk.ac.wellcome.models

import scala.util.Try

import com.fasterxml.jackson.annotation.JsonProperty

import uk.ac.wellcome.utils.JsonUtil

trait Transformable {
  def transform: Try[Work]
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
