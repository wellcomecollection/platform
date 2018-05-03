package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

import io.circe.optics.JsonPath.root
import io.circe.parser._
import cats.syntax.either._
import com.amazonaws.services.kinesis.model.InvalidArgumentException

import scala.util.Try

case class SierraRecord(id: String, data: String, modifiedDate: Instant) {
  def toBibRecord: SierraBibRecord =
    SierraBibRecord(
      id = this.id,
      data = this.data,
      modifiedDate = this.modifiedDate)

  def toItemRecord: Try[SierraItemRecord] =
    for {
      json <- parse(this.data).toTry
      bibIdsJsonSeq = root.bibIds.arr
        .getOption(json)
        .getOrElse(throw new InvalidArgumentException(
          "Json data did not contain bibIds"))
      bibIds = bibIdsJsonSeq.map { json =>
        json.asString.getOrElse(
          throw new InvalidArgumentException("Found non string in bibIds"))
      }.toList
    } yield {
      SierraItemRecord(
        id = this.id,
        data = this.data,
        modifiedDate = this.modifiedDate,
        bibIds = bibIds)
    }
}

object SierraRecord {
  def apply(id: String, data: String, modifiedDate: String): SierraRecord =
    SierraRecord(
      id = id,
      data = data,
      modifiedDate = Instant.parse(modifiedDate)
    )
}
