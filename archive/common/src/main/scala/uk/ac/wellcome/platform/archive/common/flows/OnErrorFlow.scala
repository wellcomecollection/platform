package uk.ac.wellcome.platform.archive.common.flows
import akka.stream.scaladsl.Flow
import grizzled.slf4j.Logging
import uk.ac.wellcome.platform.archive.common.models.error.ArchiveError

object OnErrorFlow extends Logging {
  def apply[T]() = {
    Flow[ArchiveError[T]].map { error =>
      warn(error.toString)
      Left(error)
    }
  }
}
