package uk.ac.wellcome.display.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.Place

@ApiModel(
  value = "Place",
  description = "A place"
)
case class DisplayPlace(
  @ApiModelProperty(
    dataType = "String"
  ) label: String
) {
  @JsonProperty("type") val ontologyType: String = "Place"
}

case object DisplayPlace {
  def apply(place: Place): DisplayPlace = DisplayPlace(
    label = place.label
  )
}
