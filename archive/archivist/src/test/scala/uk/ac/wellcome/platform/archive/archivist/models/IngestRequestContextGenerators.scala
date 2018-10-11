package uk.ac.wellcome.platform.archive.archivist.models

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models.{
  IngestBagRequest,
  StorageSpace
}
import uk.ac.wellcome.storage.ObjectLocation

trait IngestRequestContextGenerators {

  def createIngestBagRequest = createIngestBagRequestWith()

  def createIngestBagRequestWith(requestId: UUID = UUID.randomUUID(),
                                 ingestBagLocation: ObjectLocation =
                                   ObjectLocation("testNamespace", "testKey"),
                                 callbackUri: Option[URI] = None) =
    IngestBagRequest(
      requestId,
      ingestBagLocation,
      callbackUri,
      StorageSpace("fake"))
}
