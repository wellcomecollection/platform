package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.License

@ApiModel(
  value = "License",
  description =
    "The specific license under which the work in question is released to the public - for example, one of the forms of Creative Commons - if it is a precise license to which a link can be made."
)
case class DisplayLicenseV2(
  @ApiModelProperty(
    value =
      "A type of license under which the work in question is released to the public.",
    allowableValues = "cc-by, cc-by-nc, cc-by-nc-nd, cc-0, pdm"
  ) id: String,
  @ApiModelProperty(
    value = "The title or other short name of a license"
  ) label: String,
  @ApiModelProperty(
    value = "URL to the full text of a license"
  ) url: Option[String],
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "License"
)

case object DisplayLicenseV2 {
  def apply(license: License): DisplayLicenseV2 = DisplayLicenseV2(
    id = license.id,
    label = license.label,
    url = license.url
  )
}
