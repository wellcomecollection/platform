package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{LocationType, PhysicalLocation}
import uk.ac.wellcome.platform.transformer.source.{
  SierraItemData,
  SierraItemLocation
}

trait SierraLocation {
  def getLocation(itemData: SierraItemData): Option[PhysicalLocation] =
    itemData.location match {
      // We've seen records where the "location" field is populated in
      // the JSON, but the code and name are both empty strings or "none".
      // We can't do anything useful with this, so don't return a location.
      // TODO: Find out if we can populate location from other fields.
      case Some(SierraItemLocation("", ""))         => None
      case Some(SierraItemLocation("none", "none")) => None

      case Some(loc: SierraItemLocation) =>
        Some(
          PhysicalLocation(
            locationType = LocationType(loc.code),
            label = loc.name
          )
        )
      case None => None
    }
}
