package uk.ac.wellcome.platform.archive.common.models

case class BagId(
  space: StorageSpace,
  externalIdentifier: ExternalIdentifier
) {
  override def toString: String =
    s"$space/$externalIdentifier"

}
