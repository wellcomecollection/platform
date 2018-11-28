package uk.ac.wellcome.platform.sierra_reader.exceptions

import uk.ac.wellcome.exceptions.GracefulFailureException

case class SierraReaderException(e: Throwable)
    extends Exception(e.getMessage)
    with GracefulFailureException

case object SierraReaderException {
  def apply(message: String): SierraReaderException =
    SierraReaderException(new RuntimeException(message))
}
