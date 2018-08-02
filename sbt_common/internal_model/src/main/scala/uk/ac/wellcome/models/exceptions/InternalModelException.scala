package uk.ac.wellcome.models.exceptions

import uk.ac.wellcome.exceptions.GracefulFailureException

case class InternalModelException(e: Throwable) extends GracefulFailureException

case object InternalModelException {
  def apply(message: String): InternalModelException =
    InternalModelException(new RuntimeException(message))
}
