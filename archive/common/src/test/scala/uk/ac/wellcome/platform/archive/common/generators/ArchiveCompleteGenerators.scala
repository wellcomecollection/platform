package uk.ac.wellcome.platform.archive.common.generators

import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models._
import uk.ac.wellcome.storage.fixtures.S3.Bucket

trait ArchiveCompleteGenerators extends NamespaceGenerators {
  def createArchiveCompleteWith(
    archiveRequestId: UUID = randomUUID,
    space: Namespace = createNamespace,
    bagLocation: BagLocation
  ): ArchiveComplete =
    ArchiveComplete(
      archiveRequestId = archiveRequestId,
      space = space,
      bagLocation = bagLocation
    )

  def createArchiveCompleteWith(
    request: IngestBagRequest,
    bucket: Bucket,
    bagIdentifier: ExternalIdentifier
  ): ArchiveComplete =
    createArchiveCompleteWith(
      archiveRequestId = request.archiveRequestId,
      space = Namespace(request.storageSpace.underlying),
      bagLocation = BagLocation(
        storageNamespace = bucket.name,
        storagePath = "archive",
        bagPath = BagPath(s"${request.storageSpace}/$bagIdentifier")
      )
    )
}
