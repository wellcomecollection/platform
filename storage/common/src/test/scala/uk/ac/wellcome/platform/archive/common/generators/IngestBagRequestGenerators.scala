package uk.ac.wellcome.platform.archive.common.generators

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.IngestBagRequest
import uk.ac.wellcome.storage.ObjectLocation

trait IngestBagRequestGenerators extends RandomThings {
  def createIngestBagRequest: IngestBagRequest = createIngestBagRequestWith()

  def createIngestBagRequestWith(
    ingestBagLocation: ObjectLocation =
      ObjectLocation("testNamespace", "testKey")) =
    IngestBagRequest(
      id = randomUUID,
      zippedBagLocation = ingestBagLocation,
      archiveCompleteCallbackUrl = None,
      storageSpace = randomStorageSpace
    )
}
