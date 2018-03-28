package uk.ac.wellcome.platform.snapshot_convertor.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.twitter.inject.Logging
import io.circe.Decoder
import io.circe.generic.extras.Configuration
import uk.ac.wellcome.display.models.DisplayWork
import uk.ac.wellcome.models.{IdentifiedWork, WorksIncludes}
import uk.ac.wellcome.utils.JsonUtil._

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

/** Converts lines from an Elasticsearch snapshot into DisplayWorks.
  *
  * The Elasticsearch snapshot in S3 has JSON blobs, one per line, which
  * contain the internal Elasticsearch results.  This flow receives a line
  * at a time, and converts it to the corresponding DisplayWork.
  */
object ElasticsearchHitToDisplayWorkFlow extends Logging {

  case class ElasticsearchHit(
    index: String,
    documentType: String,
    id: String,
    score: Int,
    source: IdentifiedWork
  )

  implicit val config: Configuration = Configuration.default.copy(
    transformMemberNames = {
      case "index" => "_index"
      case "documentType" => "_type"
      case "id" => "_id"
      case "score" => "_score"
      case "source" => "_source"
      case other => other
    }
  )

  // TODO: Can we get a convenience wrapper for WorksIncludes that just
  // fills in everything?
  val includes = WorksIncludes(
    identifiers = true,
    thumbnail = true,
    items = true
  )

  implicit val decoder: Decoder[ElasticsearchHit] = Decoder[ElasticsearchHit]

  def apply()(implicit executionContext: ExecutionContext): Flow[String, DisplayWork, NotUsed] =
    Flow.fromFunction({ line =>
      val hit = fromJson[ElasticsearchHit](line) match {
        case Success(hit: ElasticsearchHit) => hit
        case Failure(parseFailure) => {
          warn("Failed to parse work metadata!", parseFailure)
          throw parseFailure
        }
      }

      val internalWork = hit.source
      DisplayWork(internalWork, includes = includes)
    })
}
