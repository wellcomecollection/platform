package uk.ac.wellcome.platform.archive.common.models

case class BagDigestFile(
  checksum: Checksum,
  path: BagFilePath
)
