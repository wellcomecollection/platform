package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal._

@ApiModel(
  value = "Concept"
)
sealed trait DisplayAbstractConcept extends DisplayAbstractRootConcept

case object DisplayAbstractConcept {
  def apply(abstractConcept: Displayable[AbstractConcept],
            includesIdentifiers: Boolean): DisplayAbstractConcept =
    abstractConcept match {
      case Unidentifiable(concept: Concept) =>
        DisplayConcept(
          id = None,
          identifiers = None,
          label = concept.label
        )
      case Identified(
          concept: Concept,
          canonicalId,
          sourceIdentifier,
          otherIdentifiers) =>
        DisplayConcept(
          id = Some(canonicalId),
          identifiers =
            if (includesIdentifiers)
              Some(
                (sourceIdentifier +: otherIdentifiers).map(
                  DisplayIdentifierV2(_)))
            else None,
          label = concept.label
        )
      case Unidentifiable(period: Period) =>
        DisplayPeriod(
          id = None,
          identifiers = None,
          label = period.label
        )
      case Identified(
          period: Period,
          canonicalId,
          sourceIdentifier,
          otherIdentifiers) =>
        DisplayPeriod(
          id = Some(canonicalId),
          identifiers =
            if (includesIdentifiers)
              Some(
                (sourceIdentifier +: otherIdentifiers).map(
                  DisplayIdentifierV2(_)))
            else None,
          label = period.label
        )
      case Unidentifiable(place: Place) =>
        DisplayPlace(
          id = None,
          identifiers = None,
          label = place.label
        )
      case Identified(
          place: Place,
          canonicalId,
          sourceIdentifier,
          otherIdentifiers) =>
        DisplayPlace(
          id = Some(canonicalId),
          identifiers =
            if (includesIdentifiers)
              Some(
                (sourceIdentifier +: otherIdentifiers).map(
                  DisplayIdentifierV2(_)))
            else None,
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
    dataType = "List[uk.ac.wellcome.display.models.v2.DisplayIdentifierV2]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifierV2]] = None,
  @ApiModelProperty(
    dataType = "String"
  ) label: String,
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Concept"
) extends DisplayAbstractConcept

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
    dataType = "List[uk.ac.wellcome.display.models.v2.DisplayIdentifierV2]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifierV2]] = None,
  @ApiModelProperty(
    dataType = "String"
  ) label: String,
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Period"
) extends DisplayAbstractConcept

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
    dataType = "List[uk.ac.wellcome.display.models.v2.DisplayIdentifierV2]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifierV2]] = None,
  @ApiModelProperty(
    dataType = "String"
  ) label: String,
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Place"
) extends DisplayAbstractConcept

case object DisplayPlace {
  def apply(place: Place): DisplayPlace = DisplayPlace(
    label = place.label
  )
}
