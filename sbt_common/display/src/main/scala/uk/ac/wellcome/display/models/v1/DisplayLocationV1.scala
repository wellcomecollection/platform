package uk.ac.wellcome.display.models.v1

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
    Array(classOf[DisplayDigitalLocationV1], classOf[DisplayPhysicalLocationV1])
)
sealed trait DisplayLocationV1

object DisplayLocationV1 {
  def apply(location: Location): DisplayLocationV1 = location match {
    case digitalLocation: DigitalLocation =>
      DisplayDigitalLocationV1(
        locationType = digitalLocation.locationType.id,
        url = digitalLocation.url,
        credit = digitalLocation.credit,
        license = digitalLocation.license.map(DisplayLicenseV1(_))
      )
    case l: PhysicalLocation =>
      DisplayPhysicalLocationV1(
        locationType = l.locationType.id,
        label = l.label)
  }
}

@ApiModel(
  value = "DigitalLocation",
  description = "A digital location that provides access to an item"
)
case class DisplayDigitalLocationV1(
  @ApiModelProperty(
    value = "The type of location that an item is accessible from.",
    allowableValues = "thumbnail-image, iiif-image"
  ) locationType: String,
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
  ) license: Option[DisplayLicenseV1] = None,
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") @JsonKey("type") ontologyType: String =
    "DigitalLocation"
) extends DisplayLocationV1

@ApiModel(
  value = "PhysicalLocation",
  description = "A physical location that provides access to an item"
)
case class DisplayPhysicalLocationV1(
  @ApiModelProperty(
    value = "The type of location that an item is accessible from.",
    allowableValues = ""
  ) locationType: String,
  @ApiModelProperty(
    dataType = "String",
    value = "The title or other short name of the location."
  ) label: String,
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") @JsonKey("type") ontologyType: String =
    "PhysicalLocation"
) extends DisplayLocationV1
