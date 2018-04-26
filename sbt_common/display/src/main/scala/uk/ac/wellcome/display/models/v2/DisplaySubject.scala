package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import uk.ac.wellcome.display.models.DisplayConcept
import uk.ac.wellcome.models.Subject

case class DisplaySubject(label: String,
                          concepts: List[DisplayConcept],
                          @JsonProperty("type") ontologyType: String =
                            "Subject")

object DisplaySubject {
  def apply(subject: Subject): DisplaySubject =
    DisplaySubject(label = subject.label, concepts = subject.concepts.map {
      DisplayConcept(_)
    }, ontologyType = subject.ontologyType)
}
