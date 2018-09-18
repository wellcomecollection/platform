package uk.ac.wellcome.platform.archive.archivist.models

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.models.IngestRequestContext
import uk.ac.wellcome.storage.ObjectLocation

trait IngestRequestContextGenerators {

  def createIngestRequestContext = createIngestRequestContextWith()

  def createIngestRequestContextWith(
    requestId: UUID = UUID.randomUUID(),
    ingestBagLocation: ObjectLocation =
      ObjectLocation("testNamespace", "testKey"),
    callBackUrl: Option[URI] = None) =
    IngestRequestContext(requestId, ingestBagLocation, callBackUrl)
}
