package uk.ac.wellcome.display.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal._

@ApiModel(
  value = "AbstractConcept"
)
sealed trait DisplayAbstractConcept

case object DisplayAbstractConcept {
  def apply(abstractConcept: AbstractConcept): DisplayAbstractConcept =
    abstractConcept match {
      case concept: Concept =>
        DisplayConcept(concept.label)
      case period: Period =>
        DisplayPeriod(period.label)
      case place: Place =>
        DisplayPlace(place.label)
    }
}

@ApiModel(
  value = "Concept",
  description = "A broad concept"
)
case class DisplayConcept(
  @ApiModelProperty(
    dataType = "String"
  ) label: String
) extends DisplayAbstractConcept {
  @JsonProperty("type") val ontologyType: String = "Concept"
}
case object DisplayConcept {
  def apply(concept: AbstractConcept): DisplayConcept = DisplayConcept(
    label = concept.label
  )
}

@ApiModel(
  value = "Period",
  description = "A period of time"
)
case class DisplayPeriod(
  @ApiModelProperty(
    dataType = "String"
  ) label: String
) extends DisplayAbstractConcept {
  @JsonProperty("type") val ontologyType: String = "Period"
}
case object DisplayPeriod {
  def apply(period: Period): DisplayPeriod = DisplayPeriod(
    label = period.label
  )
}

@ApiModel(
  value = "Place",
  description = "A place"
)
case class DisplayPlace(
  @ApiModelProperty(
    dataType = "String"
  ) label: String
) extends DisplayAbstractConcept {
  @JsonProperty("type") val ontologyType: String = "Place"
}
case object DisplayPlace {
  def apply(place: Place): DisplayPlace = DisplayPlace(
    label = place.label
  )
}
