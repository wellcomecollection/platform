package uk.ac.wellcome.platform.matcher.models

import uk.ac.wellcome.exceptions.GracefulFailureException

final case class VersionExpectedConflictException(
  message: String = "Version conflict!")
    extends Exception(message)

case class VersionUnexpectedConflictException(e: Throwable) extends Exception(e.getMessage) with GracefulFailureException

case object VersionUnexpectedConflictException {
  def apply(message: String): VersionUnexpectedConflictException =
    VersionUnexpectedConflictException(new IllegalStateException(message))
}
