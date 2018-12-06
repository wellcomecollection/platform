package uk.ac.wellcome.platform.transformer.sierra.transformers

import grizzled.slf4j.Logging
import uk.ac.wellcome.models.work.internal._
import uk.ac.wellcome.platform.transformer.sierra.exceptions.SierraTransformerException
import uk.ac.wellcome.platform.transformer.sierra.source.SierraItemData
import uk.ac.wellcome.platform.transformer.sierra.source.sierra.SierraSourceLocation

trait SierraLocation extends Logging {
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
            locationType = getLocationType(loc),
            label = loc.name
          )
        )
      case None => None
    }

  private def getLocationType(loc: SierraSourceLocation): LocationType =
    try {
      LocationType(loc.code)
    } catch {
      // Sometimes the cataloguers add new location types, which causes the pipeline
      // to break if we don't add them to location-types.csv.
      //
      // Rather than playing whack-a-mole, just drop a warning and guess the label we
      // would have put in the spreadsheet anyway.
      case _: IllegalArgumentException =>
        warn(
          s"No lookup provided for LocationType ${loc.code}, so using Sierra data")
        LocationType(
          id = loc.code,
          label = loc.name
        )
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
