package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal._

@ApiModel(
  value = "Subject",
  description = "A subject"
)
case class DisplaySubject(
  id: Option[String],
  identifiers: Option[List[DisplayIdentifierV2]],
  @ApiModelProperty(value = "A label given to a thing.") label: String,
  @ApiModelProperty(value = "Relates a subject to a list of concepts.") concepts: List[
    DisplayAbstractRootConcept],
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Subject"
)

object DisplaySubject {
  def apply(
    displayableSubject: Displayable[Subject[Displayable[AbstractRootConcept]]],
    includesIdentifiers: Boolean): DisplaySubject = {
    displayableSubject match {
      case Unidentifiable(subject: Subject[Displayable[AbstractRootConcept]]) =>
        DisplaySubject(
          id = None,
          identifiers = None,
          label = subject.label,
          concepts = subject.concepts.map {
            DisplayAbstractRootConcept(
              _,
              includesIdentifiers = includesIdentifiers)
          },
          ontologyType = subject.ontologyType
        )
      case Identified(
          subject: Subject[Displayable[AbstractRootConcept]],
          canonicalId,
          sourceIdentifier,
          otherIdentifiers) =>
        DisplaySubject(
          id = Some(canonicalId),
          identifiers =
            if (includesIdentifiers)
              Some(
                (sourceIdentifier +: otherIdentifiers).map(
                  DisplayIdentifierV2(_)))
            else None,
          label = subject.label,
          concepts = subject.concepts.map {
            DisplayAbstractRootConcept(
              _,
              includesIdentifiers = includesIdentifiers)
          },
          ontologyType = subject.ontologyType
        )
    }
  }
}
