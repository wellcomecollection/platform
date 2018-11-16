package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.SourceIdentifier

@ApiModel(
  value = "Identifier",
  description =
    "A unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
)
case class DisplayIdentifierV2(
  @ApiModelProperty(value =
    "Relates a Identifier to a particular authoritative source identifier scheme: for example, if the identifier is MS.49 this property might indicate that this identifier has its origins in the Wellcome Library's CALM archive management system.") identifierType: DisplayIdentifierType,
  @ApiModelProperty(value = "The value of the thing. e.g. an identifier") value: String,
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Identifier"
)

object DisplayIdentifierV2 {
  def apply(sourceIdentifier: SourceIdentifier): DisplayIdentifierV2 =
    DisplayIdentifierV2(
      identifierType =
        DisplayIdentifierType(identifierType = sourceIdentifier.identifierType),
      value = sourceIdentifier.value)
}
