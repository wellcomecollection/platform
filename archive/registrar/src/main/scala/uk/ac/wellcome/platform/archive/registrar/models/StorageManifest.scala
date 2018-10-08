package uk.ac.wellcome.platform.archive.registrar.models

import java.time.Instant

// Value classes

case class ChecksumAlgorithm( value: String)

case class BagId( value: String)

case class BagDescription( value: String)

case class BagVersion( value: Int)

case class Checksum( value: String)

case class BagFilePath( value: String)

case class BagSourceId( value: String)

// StorageManifest

case class StorageManifest(
  id: BagId,
  source: SourceIdentifier,
  identifiers: List[SourceIdentifier],
  manifest: FileManifest,
  tagManifest: TagManifest,
  locations: List[Location],
  description: Option[BagDescription] = None,
  createdDate: Instant = Instant.ofEpochMilli(0),
  lastModifiedDate: Instant = Instant.ofEpochMilli(0),
  version: BagVersion = BagVersion(1)
)

// Manifest

sealed trait Manifest {
  val checksumAlgorithm: ChecksumAlgorithm
  val files: List[BagDigestFile]
}

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
