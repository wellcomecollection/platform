package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.LocationType

@ApiModel(
  value = "LocationType"
)
case class DisplayLocationType(
  @ApiModelProperty id: String,
  @ApiModelProperty label: String,
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "LocationType"
)

object DisplayLocationType {
  def apply(locationType: LocationType): DisplayLocationType =
    DisplayLocationType(
      id = locationType.id,
      label = locationType.label
    )
}
