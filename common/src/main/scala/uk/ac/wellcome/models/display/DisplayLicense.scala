package uk.ac.wellcome.models.display

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.License

@ApiModel(
  value = "License",
  description =
    "The specific license under which the work in question is released to the public - for example, one of the forms of Creative Commons - if it is a precise license to which a link can be made."
)
case class DisplayLicense(
  @ApiModelProperty(
    value =
      "A type of license under which the work in question is released to the public.",
    allowableValues = "CC-BY, CC-BY-NC"
  ) licenseType: String,
  @ApiModelProperty(
    value = "The title or other short name of a license"
  ) label: String,
  @ApiModelProperty(
    value = "URL to the full text of a license"
  ) url: String
) {
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") val ontologyType: String = "License"
}

case object DisplayLicense {
  def apply(license: License): DisplayLicense = DisplayLicense(
    licenseType = license.licenseType,
    label = license.label,
    url = license.url
  )
}
