package uk.ac.wellcome.models.display

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.{DigitalLocation, Location, PhysicalLocation}

@ApiModel(
  value = "Location",
  description = "A location that provides access to an item",
  subTypes =
    Array(classOf[DisplayDigitalLocation], classOf[DisplayPhysicalLocation])
)
sealed trait DisplayLocation

object DisplayLocation {
  def apply(location: Location): DisplayLocation = location match {
    case l: DigitalLocation =>
      DisplayDigitalLocation(
        locationType = l.locationType,
        url = l.url,
        credit = l.credit,
        license = DisplayLicense(l.license)
      )
    case l: PhysicalLocation =>
      DisplayPhysicalLocation(locationType = l.locationType, label = l.label)
  }
}

@ApiModel(
  value = "DigitalLocation",
  description = "A digital location that provides access to an item"
)
case class DisplayDigitalLocation(
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
  ) license: DisplayLicense
) extends DisplayLocation {
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") val ontologyType: String = "DigitalLocation"
}

@ApiModel(
  value = "PhysicalLocation",
  description = "A physical location that provides access to an item"
)
case class DisplayPhysicalLocation(
  @ApiModelProperty(
    value = "The type of location that an item is accessible from.",
    allowableValues = ""
  ) locationType: String,
  @ApiModelProperty(
    dataType = "String",
    value = "The title or other short name of the location."
  ) label: String
) extends DisplayLocation {
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") val ontologyType: String = "PhysicalLocation"
}
