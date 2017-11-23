package uk.ac.wellcome.platform.api.models

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.Item

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
    dataType = "List[uk.ac.wellcome.platform.api.models.DisplayIdentifier]",
    value =
      "Relates the item to a unique system-generated identifier that governs interaction between systems and is regarded as canonical within the Wellcome data ecosystem."
  ) identifiers: Option[List[DisplayIdentifier]] = None,
  @ApiModelProperty(
    dataType = "List[uk.ac.wellcome.platform.api.models.DisplayLocation]",
    value = "List of locations that provide access to the item"
  ) locations: List[DisplayLocation] = List()
) {
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") val ontologyType: String = "Item"
}

object DisplayItem {
  def apply(item: Item, includesIdentifiers: Boolean): DisplayItem =
    DisplayItem(
      id = item.id,
      identifiers =
        if (includesIdentifiers)
          Some(item.identifiers.map(DisplayIdentifier(_)))
        else None,
      locations = item.locations.map(DisplayLocation(_))
    )
}
