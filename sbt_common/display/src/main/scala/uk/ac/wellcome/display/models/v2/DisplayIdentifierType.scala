package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.IdentifierType

@ApiModel(
  value = "IdentifierType"
)
case class DisplayIdentifierType(
  @ApiModelProperty id: String,
  @ApiModelProperty label: String,
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "IdentifierType"
)

object DisplayIdentifierType {
  def apply(identifierType: IdentifierType): DisplayIdentifierType =
    DisplayIdentifierType(
      id = identifierType.id,
      label = identifierType.label
    )
}
