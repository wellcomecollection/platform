package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.IdentifierType

@ApiModel(
  value = "IdentifierType"
)
case class DisplayIdentifierType(
  @ApiModelProperty id: String,
  @ApiModelProperty label: String
) {
  @JsonProperty("type") val ontologyType: String = "IdentifierType"
}

object DisplayIdentifierType {
  def apply(identifierType: IdentifierType): DisplayLocationType =
    DisplayLocationType(
      id = identifierType.id,
      label = identifierType.label
    )
}
