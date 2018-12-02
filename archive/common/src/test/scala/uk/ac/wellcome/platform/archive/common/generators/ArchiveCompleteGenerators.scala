package uk.ac.wellcome.platform.archive.common.generators

import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models._

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
    bagIdentifier: ExternalIdentifier
  ): ArchiveComplete =
    createArchiveCompleteWith(
      archiveRequestId = request.archiveRequestId,
      space = Namespace(request.storageSpace.underlying),
      bagLocation = BagLocation(
        storageNamespace = request.zippedBagLocation.namespace,
        storagePath = "archive",
        bagPath = BagPath(s"${request.storageSpace}/$bagIdentifier")
      )
    )
}
