package uk.ac.wellcome.platform.api.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.Location

@ApiModel(
  value = "Location",
  description = "A location that provides access to an item"
)
case class DisplayLocation(
  @ApiModelProperty(
    value = "The type of location that an item is accessible from.",
    allowableValues = "thumbnail-image, iiif-image"
  ) locationType: String,
  @ApiModelProperty(
    value = "The title or other short name of a license"
  ) url: Option[String] = None,
  @ApiModelProperty(
    value =
      "The specific license under which the work in question is released to the public - for example, one of the forms of Creative Commons - if it is a precise license to which a link can be made."
  ) license: DisplayLicense
) {
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") val ontologyType: String = "Location"
}

case object DisplayLocation {
  def apply(location: Location): DisplayLocation = DisplayLocation(
    locationType = location.locationType,
    url = location.url,
    license = DisplayLicense(location.license)
  )
}
