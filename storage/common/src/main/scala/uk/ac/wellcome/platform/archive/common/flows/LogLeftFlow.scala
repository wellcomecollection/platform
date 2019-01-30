package uk.ac.wellcome.platform.archive.common.flows

import akka.NotUsed
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.progress.models.FailedEvent

object LogLeftFlow extends Logging {
  def apply[In, Out](name: String): Flow[Either[FailedEvent[In], Out],
                                         Either[FailedEvent[In], Out],
                                         NotUsed] = {

    Flow[Either[FailedEvent[In], Out]].map {
      case left @ Left(event) => {
        val t = event.t
        val e = event.e

        warn(s"Failed processing $t@$name : ${e.getMessage}")

        left
      }
      case right => right
    }
  }
}
