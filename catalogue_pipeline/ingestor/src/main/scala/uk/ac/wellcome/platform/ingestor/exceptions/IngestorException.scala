package uk.ac.wellcome.platform.ingestor.exceptions

import uk.ac.wellcome.exceptions.GracefulFailureException

case class IngestorException(e: Throwable) extends GracefulFailureException

case object IngestorException {
  def apply(message: String): IngestorException =
    IngestorException(new RuntimeException(message))
}
