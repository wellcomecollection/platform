package uk.ac.wellcome.platform.transformer.transformers.sierra

import uk.ac.wellcome.exceptions.GracefulFailureException
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.source.{
  SierraItemData,
  SierraItemLocation
}

trait SierraLocation {
  def getPhysicalLocation(itemData: SierraItemData): Option[PhysicalLocation] =
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

  def getDigitalLocation(identifier: String): DigitalLocation = {
    if (!identifier.isEmpty) {
      DigitalLocation(
        url = s"https://wellcomelibrary.org/iiif/$identifier/manifest",
        license = None,
        locationType = LocationType("iiif-image")
      )
    } else {
      throw GracefulFailureException(
        new RuntimeException(
          "id required by DigitalLocation has not been provided"))
    }
  }
}
