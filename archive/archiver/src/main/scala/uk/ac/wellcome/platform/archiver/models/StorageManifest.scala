package uk.ac.wellcome.platform.archiver.models

import java.net.URL
import java.time.Instant

import uk.ac.wellcome.storage.ObjectLocation

// Value classes

class ChecksumAlgorithm(val underlying: String) extends AnyVal

class BagId(val underlying: String) extends AnyVal

class BagDescription(val underlying: String) extends AnyVal

class BagVersion(val underlying: Int) extends AnyVal

class Checksum(val underlying: String) extends AnyVal

class BagFilePath(val underlying: String) extends AnyVal

class BagSourceId(val underlying: String) extends AnyVal

// StorageManifest

case class StorageManifest(
                            id: BagId,
                            source: SourceIdentifier,
                            identifiers: List[SourceIdentifier],
                            manifest: Manifest,
                            tagManifest: Option[TagManifest],
                            locations: List[Location],
                            description: Option[BagDescription],
                            createdDate: Instant,
                            lastModifiedDate: Instant,
                            version: BagVersion
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
                            ontologyType: String,
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

case class PhysicalLocation(
                             locationType: LocationType,
                             label: String,
                             ontologyType: String = "PhysicalLocation"
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

// -----

case class ArchiveRequestInfo(
                               requesterId: String,
                               requestedAt: Instant,
                               callback: Callback
                             )

case class ArchiveRequestNotification(
                                       ingestLocation: ObjectLocation,
                                       requestInfo: ArchiveRequestInfo
                                     )

case class Callback(url: URL)