package uk.ac.wellcome.platform.archive.registrar.common.models

import java.time.Instant

import uk.ac.wellcome.platform.archive.common.models.{
  BagDigestFile,
  BagId,
  BagInfo,
  StorageSpace
}
import uk.ac.wellcome.platform.archive.common.progress.models.StorageLocation

case class ChecksumAlgorithm(value: String)

case class BagDescription(value: String)

case class StorageManifest(
  space: StorageSpace,
  info: BagInfo,
  manifest: FileManifest,
  tagManifest: FileManifest,
  accessLocation: StorageLocation,
  createdDate: Instant
) {
  val id = BagId(space, info.externalIdentifier)
}

case class FileManifest(checksumAlgorithm: ChecksumAlgorithm,
                        files: List[BagDigestFile])

case class SourceIdentifier(identifierType: IdentifierType,
                            ontologyType: String = "Identifier",
                            value: String)

case class IdentifierType(
  id: String,
  label: String,
)
