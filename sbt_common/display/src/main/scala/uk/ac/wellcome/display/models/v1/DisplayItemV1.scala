package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal.{
  IdentifiedItem,
  Location,
  SourceIdentifier
}

@ApiModel(
  value = "Item",
  description = "An item is a manifestation of a Work."
)
case class DisplayItemV1(
  @ApiModelProperty(
    dataType = "String",
    readOnly = true,
    value = "The canonical identifier given to a thing.") id: String,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.v1.DisplayIdentifierV1]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifierV1]] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.display.models.DisplayLocation]",
    value = "List of locations that provide access to the item"
  ) locations: List[DisplayLocationV1] = List()
) {
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") val ontologyType: String = "Item"
}

object DisplayItemV1 {
  def apply(item: IdentifiedItem,
            includesIdentifiers: Boolean): DisplayItemV1 = {
    DisplayItemV1(
      id = item.canonicalId,
      identifiers =
        if (includesIdentifiers)
          // If there aren't any identifiers on the item JSON, Jackson puts a
          // nil here.  Wrapping it in an Option casts it into a None or Some
          // as appropriate, and avoids throwing a NullPointerError when
          // we map over the value.
          Option[List[SourceIdentifier]](item.identifiers) match {
            case Some(identifiers) =>
              Some(identifiers.map(DisplayIdentifierV1(_)))
            case None => Some(List())
          } else None,
      locations = // Same as with identifiers
        Option[List[Location]](item.locations) match {
          case Some(locations) => locations.map(DisplayLocationV1(_))
          case None => List()
        }
    )
  }
}
