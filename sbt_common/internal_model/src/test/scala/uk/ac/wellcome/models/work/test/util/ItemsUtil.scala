package uk.ac.wellcome.models.work.test.util

import uk.ac.wellcome.models.work.internal._

trait ItemsUtil extends IdentifiersUtil {
  private def defaultLocation = DigitalLocation(
    locationType = LocationType("iiif-image"),
    url = "https://iiif.wellcomecollection.org/image/M0000001.jpg/info.json",
    license = License_CCBY
  )

  def createItem(
    canonicalId: String = createCanonicalId,
    sourceIdentifier: SourceIdentifier = createSourceIdentifier,
    locations: List[Location] = List(defaultLocation)
  ): Identified[Item] =
    Identified(
      canonicalId = canonicalId,
      sourceIdentifier = sourceIdentifier,
      agent = Item(locations = locations)
    )

  def createItems(count: Int): List[Identified[Item]] =
    (1 to count).map { _ =>
      createItem()
    }.toList
}
