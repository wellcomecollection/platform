package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.License

@ApiModel(
  value = "License",
  description =
    "The specific license under which the work in question is released to the public - for example, one of the forms of Creative Commons - if it is a precise license to which a link can be made."
)
case class DisplayLicenseV1(
  @ApiModelProperty(
    value =
      "A type of license under which the work in question is released to the public.",
    allowableValues = "CC-BY, CC-BY-NC, CC-BY-NC-ND, CC-0, PDM"
  ) licenseType: String,
  @ApiModelProperty(
    value = "The title or other short name of a license"
  ) label: String,
  @ApiModelProperty(
    value = "URL to the full text of a license"
  ) url: Option[String],
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "License"
)

case object DisplayLicenseV1 {
  def apply(license: License): DisplayLicenseV1 = DisplayLicenseV1(
    // The old model for License had an uppercase "licenseType" field,
    // e.g. "CC-BY" or "PDM".
    //
    // The current model uses lowercase IDs.  To preserve the V1 API,
    // we uppercase the IDs before presenting them.
    licenseType = license.id.toUpperCase,
    label = license.label,
    url = license.url
  )
}
