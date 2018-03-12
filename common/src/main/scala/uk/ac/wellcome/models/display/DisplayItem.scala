package uk.ac.wellcome.models.display

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.{IdentifiedItem, Location, SourceIdentifier}

@ApiModel(
  value = "Item",
  description = "An item is a manifestation of a Work."
)
case class DisplayItem(
  @ApiModelProperty(
    dataType = "String",
    readOnly = true,
    value = "The canonical identifier given to a thing.") id: String,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.models.display.DisplayIdentifier]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifier]] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.models.display.DisplayLocation]",
    value = "List of locations that provide access to the item"
  ) locations: List[DisplayLocation] = List()
) {
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") val ontologyType: String = "Item"
}

object DisplayItem {
  def apply(item: IdentifiedItem, includesIdentifiers: Boolean): DisplayItem = {
    DisplayItem(
      id = item.canonicalId,
      identifiers =
        if (includesIdentifiers)
          // If there aren't any identifiers on the item JSON, Jackson puts a
          // nil here.  Wrapping it in an Option casts it into a None or Some
          // as appropriate, and avoids throwing a NullPointerError when
          // we map over the value.
          Option[List[SourceIdentifier]](item.identifiers) match {
            case Some(identifiers) =>
              Some(identifiers.map(DisplayIdentifier(_)))
            case None => Some(List())
          } else None,
      locations = // Same as with identifiers
        Option[List[Location]](item.locations) match {
          case Some(locations) => locations.map(DisplayLocation(_))
          case None => List()
        }
    )
  }
}
