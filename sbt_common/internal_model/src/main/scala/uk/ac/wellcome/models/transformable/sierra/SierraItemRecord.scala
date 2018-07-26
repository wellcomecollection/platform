package uk.ac.wellcome.models.transformable.sierra

import io.circe.optics.JsonPath.root
import io.circe.parser._

import java.time.Instant

case class SierraItemRecord(
  id: String,
  data: String,
  modifiedDate: Instant,
  bibIds: List[String],
  unlinkedBibIds: List[String] = List(),
  version: Int = 0
) extends AbstractSierraRecord

case object SierraItemRecord {
  def apply(
    id: String,
    data: String,
    modifiedDate: Instant
  ): SierraItemRecord = {
    val json = parse(this.data) match {
      case Success(json) => json
      case Err(e) =>
        throw new IllegalArgumentException(s"Non-JSON data: <<$data>> ($e)")
    }

    val bibIdsJsonSeq = root
      .bibIds.arr
      .getOption(json)
      .getOrElse(
        throw new IllegalArgumentException(s"JSON data did not contain bibIds: <<$data>>"))

    val bibIds = bibIdsJsonSeq
      .map { json =>
        json.asString.getOrElse(
          throw new IllegalArgumentException("Found non string in bibIds: <<$data>>"))
      }.toList

    bibIdsJsonSeq = root.bibIds.arr
      .getOption(json)
      .getOrElse(throw new IllegalArgumentException(
        "JSON data did not contain bibIds: <<$data>>"))

    SierraItemRecord(
      id = this.id,
      data = this.data,
      modifiedDate = this.modifiedDate,
      bibIds = bibIds
    )
  }
}
