package uk.ac.wellcome.platform.storage.bags.api.models

import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.models.bagit.BagInfo

case class DisplayBagInfo(
  externalIdentifier: String,
  payloadOxum: String,
  sourceOrganization: String,
  baggingDate: String,
  externalDescription: Option[String] = None,
  internalSenderIdentifier: Option[String] = None,
  internalSenderDescription: Option[String] = None,
  @JsonKey("type")
  ontologyType: String = "BagInfo"
)

object DisplayBagInfo {
  def apply(bagInfo: BagInfo): DisplayBagInfo = DisplayBagInfo(
    externalIdentifier = bagInfo.externalIdentifier.underlying,
    payloadOxum = bagInfo.payloadOxum.toString,
    sourceOrganization = bagInfo.sourceOrganisation.underlying,
    baggingDate = bagInfo.baggingDate.toString,
    externalDescription = bagInfo.externalDescription.map(_.underlying),
    internalSenderIdentifier =
      bagInfo.internalSenderIdentifier.map(_.underlying),
    internalSenderDescription =
      bagInfo.internalSenderDescription.map(_.underlying)
  )
}
