package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.SourceIdentifier

@ApiModel(
  value = "Identifier",
  description =
    "A unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
)
case class DisplayIdentifierV1(
  @ApiModelProperty(value =
    "Relates a Identifier to a particular authoritative source identifier scheme: for example, if the identifier is MS.49 this property might indicate that this identifier has its origins in the Wellcome Library's CALM archive management system.") identifierScheme: String,
  @ApiModelProperty(value = "The value of the thing. e.g. an identifier") value: String,
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Identifier"
)

object DisplayIdentifierV1 {
  def apply(sourceIdentifier: SourceIdentifier): DisplayIdentifierV1 =
    DisplayIdentifierV1(
      identifierScheme = sourceIdentifier.identifierType.id,
      value = sourceIdentifier.value)
}
