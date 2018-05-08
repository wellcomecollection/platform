package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.Period

@ApiModel(
  value = "Period",
  description = "A period of time"
)
case class DisplayPeriodV1(
  @ApiModelProperty(
    dataType = "String"
  ) label: String
) {
  @JsonProperty("type") val ontologyType: String = "Period"
}

case object DisplayPeriodV1 {
  def apply(period: Period): DisplayPeriodV1 = DisplayPeriodV1(
    label = period.label
  )
}
