package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import uk.ac.wellcome.models.work.internal.{AbstractConcept, Displayable, Genre}

case class DisplayGenre(label: String,
                        concepts: List[DisplayAbstractConcept],
                        @JsonProperty("type") ontologyType: String = "Genre")

object DisplayGenre {
  def apply(genre: Genre[Displayable[AbstractConcept]]): DisplayGenre =
    DisplayGenre(label = genre.label, concepts = genre.concepts.map {
      DisplayAbstractConcept(_)
    }, ontologyType = genre.ontologyType)
}
