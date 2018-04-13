package uk.ac.wellcome.platform.snapshot_convertor.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.fasterxml.jackson.databind.ObjectMapper
import com.twitter.inject.Logging
import uk.ac.wellcome.display.models.v1.DisplayWorkV1

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

object DisplayWorkToJsonStringFlow extends Logging {

  def apply(mapper: ObjectMapper)(implicit executionContext: ExecutionContext)
    : Flow[DisplayWorkV1, String, NotUsed] =
    Flow.fromFunction({ work =>
      Try(mapper.writeValueAsString(work)) match {
        case Success(s: String) => s
        case Failure(parseFailure) => {
          warn(s"Failed to turn DisplayWork $work into JSON!", parseFailure)
          throw parseFailure
        }
      }
    })
}
