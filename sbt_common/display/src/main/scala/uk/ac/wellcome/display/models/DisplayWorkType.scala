package uk.ac.wellcome.display.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.WorkType

@ApiModel(
  value = "WorkType",
  description =
    "A broad, top-level description of the form of a work: namely, whether it is a printed book, archive, painting, photograph, moving image, etc."
)
case class DisplayWorkType(
  @ApiModelProperty(
    dataType = "String"
  ) id: String,
  @ApiModelProperty(
    dataType = "String"
  ) label: String,
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "WorkType"
)

case object DisplayWorkType {
  def apply(workType: WorkType): DisplayWorkType = DisplayWorkType(
    id = workType.id,
    label = workType.label
  )
}
