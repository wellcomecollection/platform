package uk.ac.wellcome.platform.api.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.Period

@ApiModel(
  value = "Period",
  description = "A period of time"
)
case class DisplayPeriod(
  @ApiModelProperty(
    dataType = "String"
  ) label: String
) {
  @JsonProperty("type") val ontologyType: String = "Period"
}

case object DisplayPeriod {
  def apply(period: Period): DisplayPeriod = DisplayPeriod(
    label = period.label
  )
}
