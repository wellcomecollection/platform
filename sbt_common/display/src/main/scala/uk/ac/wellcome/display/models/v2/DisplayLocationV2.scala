package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.{
  DigitalLocation,
  Location,
  PhysicalLocation
}

@ApiModel(
  value = "Location",
  description = "A location that provides access to an item",
  subTypes =
    Array(classOf[DisplayDigitalLocationV2], classOf[DisplayPhysicalLocationV2])
)
sealed trait DisplayLocationV2

object DisplayLocationV2 {
  def apply(location: Location): DisplayLocationV2 = location match {
    case l: DigitalLocation =>
      DisplayDigitalLocationV2(
        locationType = DisplayLocationType(l.locationType),
        url = l.url,
        credit = l.credit,
        license = l.license.map(DisplayLicenseV2(_))
      )
    case l: PhysicalLocation =>
      DisplayPhysicalLocationV2(
        locationType = DisplayLocationType(l.locationType),
        label = l.label)
  }
}

@ApiModel(
  value = "DigitalLocation",
  description = "A digital location that provides access to an item"
)
case class DisplayDigitalLocationV2(
  @ApiModelProperty(
    value = "The type of location that an item is accessible from.",
    allowableValues = "thumbnail-image, iiif-image"
  ) locationType: DisplayLocationType,
  @ApiModelProperty(
    dataType = "String",
    value = "The URL of the digital asset."
  ) url: String,
  @ApiModelProperty(
    dataType = "String",
    value = "Who to credit the image to"
  ) credit: Option[String] = None,
  @ApiModelProperty(
    value =
      "The specific license under which the work in question is released to the public - for example, one of the forms of Creative Commons - if it is a precise license to which a link can be made."
  ) license: Option[DisplayLicenseV2] = None,
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") @JsonKey("type") ontologyType: String =
    "DigitalLocation"
) extends DisplayLocationV2

@ApiModel(
  value = "PhysicalLocation",
  description = "A physical location that provides access to an item"
)
case class DisplayPhysicalLocationV2(
  @ApiModelProperty(
    value = "The type of location that an item is accessible from.",
    allowableValues = ""
  ) locationType: DisplayLocationType,
  @ApiModelProperty(
    dataType = "String",
    value = "The title or other short name of the location."
  ) label: String,
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") @JsonKey("type") ontologyType: String =
    "PhysicalLocation"
) extends DisplayLocationV2
