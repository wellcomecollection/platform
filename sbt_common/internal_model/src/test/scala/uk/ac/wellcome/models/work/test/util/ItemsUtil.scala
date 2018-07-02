package uk.ac.wellcome.models.work.test.util

import uk.ac.wellcome.models.work.internal._

import scala.util.Random

trait ItemsUtil {
  private def defaultSourceIdentifier = {
    SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      value = (Random.alphanumeric take 10 mkString) toLowerCase,
      ontologyType = "Item"
    )
  }

  private def defaultLocation = DigitalLocation(
    locationType = LocationType("iiif-image"),
    url = "https://iiif.wellcomecollection.org/image/M0000001.jpg/info.json",
    license = License_CCBY
  )

  def createItem(
    canonicalId: String = (Random.alphanumeric take 10 mkString) toLowerCase,
    sourceIdentifier: SourceIdentifier = defaultSourceIdentifier,
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
