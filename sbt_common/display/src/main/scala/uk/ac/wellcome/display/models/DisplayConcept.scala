package uk.ac.wellcome.display.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.{AbstractConcept, Concept, QualifiedConcept}

@ApiModel(
  value = "Concept",
  description = "A broad concept"
)
case class DisplayConcept(
  @ApiModelProperty(
    dataType = "String"
  ) label: String,
  @ApiModelProperty(
    dataType = "String"
  ) qualifierType: Option[String] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.DisplayConcept]"
  ) qualifiers: List[DisplayConcept] = List(),
  @ApiModelProperty(
    dataType = "uk.ac.wellcome.display.models.DisplayConcept"
  ) concept: Option[DisplayConcept] = None,
  @JsonProperty("type") val ontologyType: String = "Concept"
)

case object DisplayConcept {
  def apply(concept: AbstractConcept): DisplayConcept = concept match {
    case c: QualifiedConcept => DisplayConcept(
      label = c.label,
      concept = Some(DisplayConcept(c.concept)),
      ontologyType = c.ontologyType
    )
    case c: Concept => DisplayConcept(
      label = c.label,
      qualifierType = c.qualifierType,
      qualifiers = c.qualifiers.map { DisplayConcept(_) },
      ontologyType = c.ontologyType
    )
    case c => DisplayConcept(label = c.label)
  }
}
