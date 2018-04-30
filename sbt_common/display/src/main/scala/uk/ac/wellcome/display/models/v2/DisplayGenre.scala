package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import uk.ac.wellcome.display.models.DisplayConcept
import uk.ac.wellcome.models.work.internal.Genre

case class DisplayGenre(label: String,
                        concepts: List[DisplayConcept],
                        @JsonProperty("type") ontologyType: String = "Genre")

object DisplayGenre {
  def apply(genre: Genre): DisplayGenre =
    DisplayGenre(label = genre.label, concepts = genre.concepts.map {
      DisplayConcept(_)
    }, ontologyType = genre.ontologyType)
}
