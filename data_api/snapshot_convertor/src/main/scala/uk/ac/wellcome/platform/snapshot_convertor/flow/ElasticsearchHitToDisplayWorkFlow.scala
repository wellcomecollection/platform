package uk.ac.wellcome.platform.snapshot_convertor.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.twitter.inject.Logging
import io.circe.Decoder
import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.models.{IdentifiedWork, WorksIncludes}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

case class ElasticsearchHit(
  @JsonKey("_index") index: String,
  @JsonKey("_type") documentType: String,
  @JsonKey("_id") id: String,
  @JsonKey("_score") score: Int,
  @JsonKey("_source") source: IdentifiedWork
)

/** Converts lines from an Elasticsearch snapshot into DisplayWorks.
  *
  * The Elasticsearch snapshot in S3 has JSON blobs, one per line, which
  * contain the internal Elasticsearch results.  This flow receives a line
  * at a time, and converts it to the corresponding DisplayWork.
  */
object ElasticsearchHitToDisplayWorkFlow extends Logging {

  // TODO: Can we get a convenience wrapper for WorksIncludes that just
  // fills in everything?
  val includes = WorksIncludes(
    identifiers = true,
    thumbnail = true,
    items = true
  )

  def apply()(implicit executionContext: ExecutionContext)
    : Flow[String, DisplayWork, NotUsed] =
    Flow.fromFunction({ line =>
      val hit = fromJson[ElasticsearchHit](line) match {
        case Success(h: ElasticsearchHit) => h
        case Failure(parseFailure) => {
          warn("Failed to parse work metadata!", parseFailure)
          throw parseFailure
        }
      }

      val internalWork = hit.source
      DisplayWork(internalWork, includes = includes)
    })
}
