package uk.ac.wellcome.models

import uk.ac.wellcome.models.transformable._
import uk.ac.wellcome.utils.JsonUtil

import scala.util.Try

case class CalmTransformableData(
  AccessStatus: Array[String]
) extends Transformable

//TODO add some tests around transformation
case class CalmTransformable(
  RecordID: String,
  RecordType: String,
  AltRefNo: String,
  RefNo: String,
  data: String,
  ReindexShard: String = "default",
  ReindexVersion: Int = 0
) extends Transformable
    with Reindexable[String] {

  val id: ItemIdentifier[String] = ItemIdentifier(
    HashKey("RecordID", RecordID),
    RangeKey("RecordType", RecordType)
  )

}
