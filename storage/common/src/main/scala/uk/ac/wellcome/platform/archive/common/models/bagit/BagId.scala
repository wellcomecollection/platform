package uk.ac.wellcome.platform.archive.common.models.bagit

import uk.ac.wellcome.platform.archive.common.models.StorageSpace

case class BagId(
  space: StorageSpace,
  externalIdentifier: ExternalIdentifier
) {
  override def toString: String =
    s"$space/$externalIdentifier"

}
