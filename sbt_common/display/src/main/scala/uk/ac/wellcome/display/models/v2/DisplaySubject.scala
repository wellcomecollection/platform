package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.{
  AbstractRootConcept,
  Displayable,
  Subject
}

@ApiModel(
  value = "Subject",
  description = "A subject"
)
case class DisplaySubject(
  @ApiModelProperty(value = "A label given to a thing.") label: String,
  @ApiModelProperty(value = "Relates a subject to a list of concepts.") concepts: List[
    DisplayAbstractRootConcept],
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Subject"
)

object DisplaySubject {
  def apply(subject: Subject[Displayable[AbstractRootConcept]],
            includesIdentifiers: Boolean): DisplaySubject =
    DisplaySubject(
      label = subject.label,
      concepts = subject.concepts.map {
        DisplayAbstractRootConcept(_, includesIdentifiers = includesIdentifiers)
      },
      ontologyType = subject.ontologyType
    )
}
