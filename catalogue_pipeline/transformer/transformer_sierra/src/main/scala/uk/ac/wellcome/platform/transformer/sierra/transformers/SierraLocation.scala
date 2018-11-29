package uk.ac.wellcome.platform.transformer.sierra.transformers

import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.exceptions.SierraTransformerException
import uk.ac.wellcome.platform.transformer.sierra.source.SierraItemData
import uk.ac.wellcome.platform.transformer.sierra.source.sierra.SierraSourceLocation

trait SierraLocation {
  def getPhysicalLocation(itemData: SierraItemData): Option[PhysicalLocation] =
    itemData.location match {
      // We've seen records where the "location" field is populated in
      // the JSON, but the code and name are both empty strings or "none".
      // We can't do anything useful with this, so don't return a location.
      // TODO: Find out if we can populate location from other fields.
      case Some(SierraSourceLocation("", ""))         => None
      case Some(SierraSourceLocation("none", "none")) => None

      case Some(loc: SierraSourceLocation) =>
        Some(
          PhysicalLocation(
            locationType = LocationType(loc.code),
            label = loc.name
          )
        )
      case None => None
    }

  def getDigitalLocation(identifier: String): DigitalLocation = {
    // This is a defensive check, it may not be needed since an identifier should always be present.
    if (!identifier.isEmpty) {
      DigitalLocation(
        url = s"https://wellcomelibrary.org/iiif/$identifier/manifest",
        license = None,
        locationType = LocationType("iiif-presentation")
      )
    } else {
      throw SierraTransformerException(
        "id required by DigitalLocation has not been provided")
    }
  }
}
