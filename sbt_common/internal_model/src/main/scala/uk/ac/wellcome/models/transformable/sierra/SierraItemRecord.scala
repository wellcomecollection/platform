package uk.ac.wellcome.models.transformable.sierra

import java.time.Instant

import uk.ac.wellcome.json.JsonUtil._

import scala.util.{Failure, Success}

case class SierraItemRecord(
  id: SierraItemNumber,
  data: String,
  modifiedDate: Instant,
  bibIds: List[SierraBibNumber],
  unlinkedBibIds: List[SierraBibNumber] = List()
) extends AbstractSierraRecord

case object SierraItemRecord {

  private case class SierraAPIData(bibIds: List[String])

  /** This apply method is for parsing JSON bodies that come from the
    * Sierra API.
    */
  def apply(id: String,
            data: String,
            modifiedDate: Instant): SierraItemRecord = {
    val bibIds = fromJson[SierraAPIData](data) match {
      case Success(apiData) => apiData.bibIds
      case Failure(e) =>
        throw new IllegalArgumentException(
          s"Error parsing bibIds from JSON <<$data>> ($e)")
    }

    SierraItemRecord(
      id = SierraItemNumber(id),
      data = data,
      modifiedDate = modifiedDate,
      bibIds = bibIds.map { SierraBibNumber }
    )
  }
}
