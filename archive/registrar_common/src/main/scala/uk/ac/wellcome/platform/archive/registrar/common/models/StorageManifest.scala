package uk.ac.wellcome.platform.archive.registrar.common.models

import java.time.Instant

import uk.ac.wellcome.platform.archive.common.models.BagId

case class ChecksumAlgorithm(value: String)

case class BagDescription(value: String)

case class Checksum(value: String)

case class BagFilePath(value: String)

case class StorageManifest(
  id: BagId,
  manifest: FileManifest,
  createdDate: Instant
)


case class FileManifest(
  checksumAlgorithm: ChecksumAlgorithm,
  files: List[BagDigestFile])

case class SourceIdentifier(identifierType: IdentifierType,
                            ontologyType: String = "Identifier",
                            value: String)

case class IdentifierType(
  id: String,
  label: String,
)

case class BagDigestFile(
  checksum: Checksum,
  path: BagFilePath
)
