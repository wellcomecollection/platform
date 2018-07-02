package uk.ac.wellcome.models.work.test.util

import uk.ac.wellcome.models.work.internal._

import scala.util.Random

trait ItemsUtil {
  private def sourceIdentifier = {
    SourceIdentifier(
      identifierType = IdentifierType("miro-image-number"),
      value = (Random.alphanumeric take 10 mkString) toLowerCase,
      ontologyType = "Item"
    )
  }

  private def location = DigitalLocation(
    locationType = LocationType("iiif-image"),
    url = "https://iiif.wellcomecollection.org/image/M0000001.jpg/info.json",
    license = License_CCBY
  )

  private def createItem: Identified[Item] =
    Identified(
      canonicalId = (Random.alphanumeric take 10 mkString) toLowerCase,
      sourceIdentifier = sourceIdentifier,
      agent = Item(locations = List(location))
    )

  def createItems(count: Int): List[Identified[Item]] =
    (1 to count)
      .map { _ => createItem }
      .toList
}
