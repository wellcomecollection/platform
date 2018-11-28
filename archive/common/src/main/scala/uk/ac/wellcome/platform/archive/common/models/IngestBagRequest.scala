package uk.ac.wellcome.platform.archive.common.models

import java.net.URI
import java.util.UUID

import uk.ac.wellcome.platform.archive.common.json.{
  URIConverters,
  UUIDConverters
}
import uk.ac.wellcome.storage.ObjectLocation

case class IngestBagRequest(archiveRequestId: UUID,
                            zippedBagLocation: ObjectLocation,
                            archiveCompleteCallbackUrl: Option[URI] = None,
                            storageSpace: Namespace)

object IngestBagRequest extends URIConverters with UUIDConverters {}
