package uk.ac.wellcome.display.models.v2

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
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
  ) locations: List[DisplayLocationV2] = List(),
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Item"
)

object DisplayItemV2 {
  def apply(item: Displayable[Item],
            includesIdentifiers: Boolean): DisplayItemV2 = {
    item match {
      case identifiedItem: Identified[Item] =>
        DisplayItemV2(
          id = Some(identifiedItem.canonicalId),
          identifiers =
            if (includesIdentifiers)
              Some(identifiedItem.identifiers.map { DisplayIdentifierV2(_) })
            else None,
          locations = identifiedItem.agent.locations.map {
            DisplayLocationV2(_)
          }
        )
      case unidentifiableItem: Unidentifiable[Item] =>
        DisplayItemV2(
          id = None,
          identifiers = None,
          locations = unidentifiableItem.agent.locations.map {
            DisplayLocationV2(_)
          }
        )
    }

  }
}
