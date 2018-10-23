package uk.ac.wellcome.platform.archive.registrar.http.models
import io.circe.generic.extras.JsonKey
import uk.ac.wellcome.platform.archive.common.models.BagInfo

case class DisplayBagInfo(
  externalIdentifier: String,
  payloadOxum: String,
  sourceOrganization: String,
  baggingDate: String,
  @JsonKey("type")
  ontologyType: String = "BagInfo"
)

object DisplayBagInfo {
  def apply(bagInfo: BagInfo): DisplayBagInfo = DisplayBagInfo(
    externalIdentifier = bagInfo.externalIdentifier.underlying,
    payloadOxum = bagInfo.payloadOxum.toString,
    sourceOrganization = bagInfo.sourceOrganisation.underlying,
    baggingDate = bagInfo.baggingDate.toString)
}
