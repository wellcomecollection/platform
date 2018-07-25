package uk.ac.wellcome.platform.matcher.models

final case class VersionExpectedConflictException(
  message: String = "Version conflict!")
    extends Exception(message)

final case class VersionUnexpectedConflictException(
  message: String = "Version conflict!")
    extends Exception(message)
