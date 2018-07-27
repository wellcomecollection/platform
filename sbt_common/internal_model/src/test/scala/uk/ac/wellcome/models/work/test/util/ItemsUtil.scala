package uk.ac.wellcome.models.work.test.util

import uk.ac.wellcome.models.work.internal.{DigitalLocation, _}

trait ItemsUtil extends IdentifiersUtil {
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

  def createIdentifiableItemWith(
    sourceIdentifier: SourceIdentifier = createSourceIdentifier,
    locations: List[Location] = List(defaultLocation)
  ): Identifiable[Item] =
    Identifiable(
      sourceIdentifier = sourceIdentifier,
      agent = Item(locations = locations)
    )

  def createPhysicalLocation = createPhysicalLocationWith()

  def createPhysicalLocationWith(locationType: LocationType =
                                   createStoresLocationType,
                                 label: String = "locationLabel") =
    PhysicalLocation(locationType, label)

  def createDigitalLocation = createDigitalLocationWith()

  def createDigitalLocationWith(
    locationType: LocationType = createPresentationLocationType,
    url: String = defaultLocationUrl,
    license: License = License_CCBY) = DigitalLocation(
    locationType = locationType,
    url = url,
    license = Some(license)
  )

  private def defaultLocation = createDigitalLocationWith()

  private def defaultLocationUrl =
    "https://iiif.wellcomecollection.org/image/M0000001.jpg/info.json"

  def createImageLocationType = LocationType("iiif-image")

  def createPresentationLocationType = LocationType("iiif-presentation")

  def createStoresLocationType = LocationType("sgmed")
}
