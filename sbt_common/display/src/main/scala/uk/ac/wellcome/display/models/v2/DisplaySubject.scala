package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.{AbstractConcept, Displayable, Subject}

@ApiModel(
  value = "Subject",
  description = "A subject"
)
case class DisplaySubject(@ApiModelProperty(
  value = "A label given to a thing.")
                           label: String,
                          @ApiModelProperty(
                            value = "Relates a subject to a list of concepts.")
                          concepts: List[DisplayAbstractConcept],
                          @JsonProperty("type") ontologyType: String =
                            "Subject")

object DisplaySubject {
  def apply(subject: Subject[Displayable[AbstractConcept]]): DisplaySubject =
    DisplaySubject(label = subject.label, concepts = subject.concepts.map {
      DisplayAbstractConcept(_)
    }, ontologyType = subject.ontologyType)
}
