package uk.ac.wellcome.models.transformable.sierra

import uk.ac.wellcome.utils.JsonUtil._

import java.time.Instant
import scala.util.{Failure, Success}

case class SierraItemRecord(
  id: String,
  data: String,
  modifiedDate: Instant,
  bibIds: List[String],
  unlinkedBibIds: List[String] = List(),
  version: Int = 0
) extends AbstractSierraRecord

case object SierraItemRecord {

  /** This apply method is for parsing JSON bodies that come from the
    * Sierra API.
    */
  def apply(
    id: String,
    data: String,
    modifiedDate: Instant
  ): SierraItemRecord = {

    case class SierraAPIData(bibIds: List[String])

    val bibIds = fromJson[SierraAPIData](data) match {
      case Success(apiData) => apiData.bibIds
      case Failure(e) =>
        throw new IllegalArgumentException(s"Error parsing bibIds from JSON <<$data>> ($e)")
    }

    SierraItemRecord(
      id = id,
      data = data,
      modifiedDate = modifiedDate,
      bibIds = bibIds
    )
  }
}
