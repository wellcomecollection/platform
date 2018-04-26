package uk.ac.wellcome.transformer.transformers.sierra

import uk.ac.wellcome.transformer.source.SierraItemData
import uk.ac.wellcome.work_model.PhysicalLocation

trait SierraLocation {
  def getLocation(itemData: SierraItemData): Option[PhysicalLocation] =
    itemData.location.map { l =>
      PhysicalLocation(locationType = l.code, label = l.name)
    }
}
