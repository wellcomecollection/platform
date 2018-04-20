package uk.ac.wellcome.platform.snapshot_convertor.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.twitter.inject.Logging
import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.models.IdentifiedWork
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

/** Converts lines from an Elasticsearch snapshot into IdentifiedWork.
  *
  * The Elasticsearch snapshot in S3 has JSON blobs, one per line, which
  * contain the internal Elasticsearch results.  This flow receives a line
  * at a time, and converts it to the corresponding IdentifiedWork.
  */
object ElasticsearchHitToIdentifiedWorkFlow extends Logging {

  def apply()(implicit executionContext: ExecutionContext)
    : Flow[String, IdentifiedWork, NotUsed] =
    Flow.fromFunction({ line =>
      val hit = fromJson[ElasticsearchHit](line) match {
        case Success(h: ElasticsearchHit) => h
        case Failure(parseFailure) => {
          warn("Failed to parse work metadata!", parseFailure)
          throw parseFailure
        }
      }

      hit.source
    })
}
