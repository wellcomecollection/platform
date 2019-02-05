package uk.ac.wellcome.platform.archive.common.models.bagit

case class BagPath(underlying: String) extends AnyVal {
  override def toString: String = underlying
}
