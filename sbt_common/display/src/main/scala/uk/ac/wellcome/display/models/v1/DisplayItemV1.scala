package uk.ac.wellcome.display.models.v1

import com.fasterxml.jackson.annotation.JsonProperty
import io.circe.generic.extras.JsonKey
import io.swagger.annotations.{ApiModel, ApiModelProperty}
import uk.ac.wellcome.models.work.internal._

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
    value = "List of locations that provide access to the item"
  ) locations: List[DisplayLocationV1] = List(),
  @ApiModelProperty(readOnly = true, value = "A type of thing")
  @JsonProperty("type") @JsonKey("type") ontologyType: String = "Item"
)

object DisplayItemV1 {
  def apply(item: Identified[Item],
            includesIdentifiers: Boolean): DisplayItemV1 = {
    DisplayItemV1(
      id = item.canonicalId,
      identifiers =
        if (includesIdentifiers)
          Some(item.identifiers.map { DisplayIdentifierV1(_) })
        else None,
      locations = item.agent.locations.map { DisplayLocationV1(_) }
    )
  }
}
