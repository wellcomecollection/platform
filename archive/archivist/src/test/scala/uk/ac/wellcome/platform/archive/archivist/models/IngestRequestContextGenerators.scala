package uk.ac.wellcome.platform.archive.archivist.models

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest
import uk.ac.wellcome.storage.ObjectLocation

trait IngestRequestContextGenerators extends RandomThings {
  def createIngestBagRequest: IngestBagRequest = createIngestBagRequestWith()

  def createIngestBagRequestWith(requestId: UUID = randomUUID,
                                 ingestBagLocation: ObjectLocation =
                                   ObjectLocation("testNamespace", "testKey"),
                                 callbackUri: Option[URI] = None) =
    IngestBagRequest(
      archiveRequestId = requestId,
      zippedBagLocation = ingestBagLocation,
      archiveCompleteCallbackUrl = callbackUri,
      storageSpace = createNamespace
    )
}
