package uk.ac.wellcome.display.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.SourceIdentifier

@ApiModel(
  value = "Identifier",
  description =
    "A unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
)
case class DisplayIdentifier(
  @ApiModelProperty(value =
    "Relates a Identifier to a particular authoritative source identifier scheme: for example, if the identifier is MS.49 this property might indicate that this identifier has its origins in the Wellcome Library's CALM archive management system.") identifierScheme: String,
  @ApiModelProperty(value = "The value of the thing. e.g. an identifier") value: String) {
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") val ontologyType: String = "Identifier"
}

object DisplayIdentifier {
  def apply(sourceIdentifier: SourceIdentifier): DisplayIdentifier =
    DisplayIdentifier(
      identifierScheme = sourceIdentifier.identifierScheme.toString,
      value = sourceIdentifier.value)
}
