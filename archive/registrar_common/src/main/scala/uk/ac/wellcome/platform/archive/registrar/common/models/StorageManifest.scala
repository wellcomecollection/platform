package uk.ac.wellcome.platform.archive.registrar.common.models

import java.time.Instant

import uk.ac.wellcome.platform.archive.common.models.BagId

// Value classes

case class ChecksumAlgorithm(value: String)

case class BagDescription(value: String)

case class BagVersion(value: Int)

case class Checksum(value: String)

case class BagFilePath(value: String)

// StorageManifest

case class StorageManifest(
  id: BagId,
  manifest: FileManifest,
  createdDate: Instant,
  lastModifiedDate: Instant,
  version: BagVersion
)

// Manifest

case class FileManifest(
  checksumAlgorithm: ChecksumAlgorithm,
  files: List[BagDigestFile]
)

case class TagManifest(
  checksumAlgorithm: ChecksumAlgorithm,
  files: List[BagDigestFile]
)

// Identifier

case class SourceIdentifier(identifierType: IdentifierType,
                            ontologyType: String = "Identifier",
                            value: String)

case class IdentifierType(
  id: String,
  label: String,
)

// Location

sealed trait Location {
  val locationType: LocationType
}

case class DigitalLocation(
  url: String,
  locationType: LocationType,
  ontologyType: String = "DigitalLocation"
) extends Location

case class LocationType(
  id: String,
  label: String,
  ontologyType: String = "LocationType"
)

// Bag digest file

case class BagDigestFile(
  checksum: Checksum,
  path: BagFilePath
)
