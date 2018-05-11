package uk.ac.wellcome.platform.snapshot_generator.flow

import akka.NotUsed
import akka.stream.scaladsl.Flow
import com.fasterxml.jackson.databind.ObjectMapper
import com.twitter.inject.Logging
import uk.ac.wellcome.display.models.DisplayWork

import scala.util.{Failure, Success, Try}

object DisplayWorkToJsonStringFlow extends Logging {

  def apply(mapper: ObjectMapper): Flow[DisplayWork, String, NotUsed] =
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
