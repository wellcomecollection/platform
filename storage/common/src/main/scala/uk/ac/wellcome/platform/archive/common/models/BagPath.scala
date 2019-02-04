package uk.ac.wellcome.platform.archive.common.models

case class BagPath(value: String) {
  override def toString: String = value
}

case object BagPath {
  def apply(externalIdentifier: ExternalIdentifier): BagPath =
    BagPath(externalIdentifier.toString)
}
