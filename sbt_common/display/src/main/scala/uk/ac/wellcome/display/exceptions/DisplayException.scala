package uk.ac.wellcome.display.exceptions

import uk.ac.wellcome.exceptions.GracefulFailureException

case class DisplayException(e: Throwable) extends GracefulFailureException

case object DisplayException {
  def apply(message: String): DisplayException =
    DisplayException(new RuntimeException(message))
}
