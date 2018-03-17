package uk.ac.wellcome.display.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.{Place, WorkType}

@ApiModel(
  value = "Place",
  description = "A place"
)
case class DisplayWorkType(
  @ApiModelProperty(
    dataType = "String"
  ) id: String,
  @ApiModelProperty(
    dataType = "String"
  ) label: String
) {
  @JsonProperty("type") val ontologyType: String = "WorkType"
}

case object DisplayWorkType {
  def apply(workType: WorkType): DisplayWorkType = DisplayWorkType(
    id = workType.id,
    label = workType.label
  )
}
