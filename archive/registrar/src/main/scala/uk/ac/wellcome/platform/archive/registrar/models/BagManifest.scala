package uk.ac.wellcome.platform.archive.registrar.models

import java.time.Instant

// Value classes

case class ChecksumAlgorithm(val value: String)

case class BagId(val value: String)

case class BagDescription(val value: String)

case class BagVersion(val value: Int)

case class Checksum(val value: String)

case class BagFilePath(val value: String)

case class BagSourceId(val value: String)

// StorageManifest

case class BagManifest(
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
  files: Iterable[BagDigestFile]
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

object BagDigestFile {
  def apply(checksum: String, filePath: String): BagDigestFile = {
    BagDigestFile(
      Checksum(checksum),
      BagFilePath(filePath)
    )
  }

}
