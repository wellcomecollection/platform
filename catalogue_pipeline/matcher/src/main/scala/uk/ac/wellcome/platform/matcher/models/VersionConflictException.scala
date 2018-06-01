package uk.ac.wellcome.platform.matcher.models

final case class VersionConflictException(message: String = "Version conflict!")
  extends Exception(message)
