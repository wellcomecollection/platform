package uk.ac.wellcome.platform.archive.common.models.bagit

// TODO: This should take in a string, not an ExternalIdentifier
case class BagPath(value: ExternalIdentifier) {
  override def toString: String = value.toString
}
