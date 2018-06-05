package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.models.work.internal.{LocationType, PhysicalLocation}
import uk.ac.wellcome.platform.transformer.source.SierraItemData

trait SierraLocation {
  def getLocation(itemData: SierraItemData): Option[PhysicalLocation] =
    itemData.location.map { l =>
      PhysicalLocation(locationType = LocationType(l.code), label = l.name)
    }
}
