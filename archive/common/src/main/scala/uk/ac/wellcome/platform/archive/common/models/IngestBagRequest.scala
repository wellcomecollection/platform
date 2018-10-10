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
                            storageSpace: StorageSpace
                           )

case class StorageSpace(underlying: String) extends AnyVal {
  override def toString: String = underlying
}

object IngestBagRequest extends URIConverters with UUIDConverters
