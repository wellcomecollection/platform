package uk.ac.wellcome.platform.archive.common.models.bagit

case class BagDigestFile(
  checksum: String,
  path: BagItemPath
)
