package uk.ac.wellcome.platform.matcher.models

import uk.ac.wellcome.exceptions.GracefulFailureException

final case class VersionExpectedConflictException(
  message: String = "Version conflict!")
    extends Exception(message)

final case class VersionUnexpectedConflictException(e: Throwable)
    extends GracefulFailureException {
  override def getMessage = e.getMessage
}

case object VersionUnexpectedConflictException {
  def apply(message: String): VersionUnexpectedConflictException =
    VersionUnexpectedConflictException(new IllegalStateException(message))
}
