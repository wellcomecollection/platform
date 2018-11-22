package uk.ac.wellcome.display.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.Language

@ApiModel(
  value = "Language",
  description =
    "A language recognised as one of those in the ISO 639-2 language codes."
)
case class DisplayLanguage(
  @ApiModelProperty(
    value = "An ISO 639-2 language code."
  ) id: String,
  @ApiModelProperty(
    value = "The name of a language"
  ) label: String,
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Language"
)

case object DisplayLanguage {
  def apply(language: Language): DisplayLanguage = DisplayLanguage(
    id = language.id,
    label = language.label
  )
}
