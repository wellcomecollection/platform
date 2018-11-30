package uk.ac.wellcome.platform.archive.common.generators

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.fixtures.RandomThings
import uk.ac.wellcome.platform.archive.common.models.{
  IngestBagRequest,
  StorageSpace
}
import uk.ac.wellcome.storage.ObjectLocation

trait IngestBagRequestGenerators extends RandomThings {
  def createIngestBagRequest: IngestBagRequest = createIngestBagRequestWith()

  def createIngestBagRequestWith(requestId: UUID = randomUUID,
                                 ingestBagLocation: ObjectLocation =
                                   ObjectLocation("testNamespace", "testKey"),
                                 callbackUri: Option[URI] = None) =
    IngestBagRequest(
      requestId,
      ingestBagLocation,
      callbackUri,
      StorageSpace("fake"))
}
