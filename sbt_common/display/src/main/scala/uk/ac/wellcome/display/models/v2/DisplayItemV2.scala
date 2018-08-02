package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal._

@ApiModel(
  value = "Item",
  description = "An item is a manifestation of a Work."
)
case class DisplayItemV2(
  @ApiModelProperty(
    dataType = "String",
    readOnly = true,
    value = "The canonical identifier given to a thing.") id: Option[String],
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.v2.DisplayIdentifierV2]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifierV2]] = None,
  @ApiModelProperty(
    value = "List of locations that provide access to the item"
  ) locations: List[DisplayLocationV2] = List()
) {
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") val ontologyType: String = "Item"
}

object DisplayItemV2 {
  def apply(item: Displayable[Item],
            includesIdentifiers: Boolean): DisplayItemV2 = {
    item match {
      case identifiedItem: Identified[Item] =>
        DisplayItemV2(
          id = Some(identifiedItem.canonicalId),
          identifiers =
            if (includesIdentifiers)
              // If there aren't any identifiers on the item JSON, Jackson puts a
              // nil here.  Wrapping it in an Option casts it into a None or Some
              // as appropriate, and avoids throwing a NullPointerError when
              // we map over the value.
              Option[List[SourceIdentifier]](identifiedItem.identifiers) match {
                case Some(identifiers) =>
                  Some(identifiers.map(DisplayIdentifierV2(_)))
                case None => Some(List())
              } else None,
          locations = // Same as with identifiers
            Option[List[Location]](identifiedItem.agent.locations) match {
              case Some(locations) => locations.map(DisplayLocationV2(_))
              case None            => List()
            }
        )
      case unidientifiableItem: Unidentifiable[Item] => DisplayItemV2(
        id = None,
        identifiers = None,
        locations =
          Option[List[Location]](unidientifiableItem.agent.locations) match {
            case Some(locations) => locations.map(DisplayLocationV2(_))
            case None            => List()
          }
      )
    }

  }
}
