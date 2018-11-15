package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.{AbstractConcept, Displayable, Genre}

@ApiModel(
  value = "Genre",
  description = "A genre"
)
case class DisplayGenre(
  @ApiModelProperty(value = "A label given to a thing.") label: String,
  @ApiModelProperty(value = "Relates a genre to a list of concepts.") concepts: List[
    DisplayAbstractConcept],
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Genre"
)

object DisplayGenre {
  def apply(genre: Genre[Displayable[AbstractConcept]],
            includesIdentifiers: Boolean): DisplayGenre =
    DisplayGenre(label = genre.label, concepts = genre.concepts.map {
      DisplayAbstractConcept(_, includesIdentifiers = includesIdentifiers)
    }, ontologyType = genre.ontologyType)
}
