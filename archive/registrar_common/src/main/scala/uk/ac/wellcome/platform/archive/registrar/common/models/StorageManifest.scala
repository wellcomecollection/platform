package uk.ac.wellcome.platform.archive.registrar.common.models

import java.time.Instant

import uk.ac.wellcome.platform.archive.common.models.BagId
import uk.ac.wellcome.storage.ObjectLocation

case class ChecksumAlgorithm(value: String)

case class BagDescription(value: String)

case class Checksum(value: String)

case class BagFilePath(value: String)

case class StorageManifest(
  id: BagId,
  manifest: FileManifest,
  accessLocation: Location,
  createdDate: Instant
)

case class FileManifest(checksumAlgorithm: ChecksumAlgorithm,
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

case class Location(provider: Provider, location: ObjectLocation)

case class Provider(id: String, label: String)
