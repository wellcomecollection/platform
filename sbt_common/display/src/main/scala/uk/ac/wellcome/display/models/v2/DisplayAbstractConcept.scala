package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.display.models._
import uk.ac.wellcome.models.work.internal._

@ApiModel(
  value = "Concept"
)
sealed trait DisplayAbstractConcept

case object DisplayAbstractConcept {
  def apply(
    abstractConcept: Displayable[AbstractConcept]): DisplayAbstractConcept =
    abstractConcept match {
      case Unidentifiable(concept: Concept) =>
        DisplayConcept(
          id = None,
          identifiers = None,
          label = concept.label
        )
      case Identified(concept: Concept, id, identifiers) =>
        DisplayConcept(
          id = Some(id),
          identifiers = Some(identifiers.map { DisplayIdentifier(_) }),
          label = concept.label
        )
      case Unidentifiable(period: Period) =>
        DisplayPeriod(
          id = None,
          identifiers = None,
          label = period.label
        )
      case Identified(period: Period, id, identifiers) =>
        DisplayPeriod(
          id = Some(id),
          identifiers = Some(identifiers.map { DisplayIdentifier(_) }),
          label = period.label
        )
      case Unidentifiable(place: Place) =>
        DisplayPlace(
          id = None,
          identifiers = None,
          label = place.label
        )
      case Identified(place: Place, id, identifiers) =>
        DisplayPlace(
          id = Some(id),
          identifiers = Some(identifiers.map { DisplayIdentifier(_) }),
          label = place.label
        )
    }
}

@ApiModel(
  value = "Concept",
  description = "A broad concept"
)
case class DisplayConcept(
  @ApiModelProperty(
    dataType = "String",
    value = "The canonical identifier given to a thing"
  ) id: Option[String] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.DisplayIdentifier]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifier]] = None,
  @ApiModelProperty(
    dataType = "String"
  ) label: String
) extends DisplayAbstractConcept {
  @JsonProperty("type") val ontologyType: String = "Concept"
}

@ApiModel(
  value = "Period",
  description = "A period of time"
)
case class DisplayPeriod(
  @ApiModelProperty(
    dataType = "String",
    value = "The canonical identifier given to a thing"
  ) id: Option[String] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.DisplayIdentifier]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifier]] = None,
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
    dataType = "String",
    value = "The canonical identifier given to a thing"
  ) id: Option[String] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.DisplayIdentifier]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifier]] = None,
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
