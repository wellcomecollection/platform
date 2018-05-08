package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.Place

@ApiModel(
  value = "Place",
  description = "A place"
)
case class DisplayPlaceV1(
  @ApiModelProperty(
    dataType = "String"
  ) label: String
) {
  @JsonProperty("type") val ontologyType: String = "Place"
}

case object DisplayPlaceV1 {
  def apply(place: Place): DisplayPlaceV1 = DisplayPlaceV1(
    label = place.label
  )
}
