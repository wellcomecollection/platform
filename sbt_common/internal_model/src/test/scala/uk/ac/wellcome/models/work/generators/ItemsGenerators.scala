package uk.ac.wellcome.models.work.generators

import uk.ac.wellcome.models.work.internal.{DigitalLocation, _}

trait ItemsGenerators extends IdentifiersGenerators {
  def createIdentifiedItemWith(
    canonicalId: String = createCanonicalId,
    sourceIdentifier: SourceIdentifier = createSourceIdentifier,
    locations: List[Location] = List(defaultLocation)
  ): Identified[Item] =
    Identified(
      canonicalId = canonicalId,
      sourceIdentifier = sourceIdentifier,
      agent = Item(locations = locations)
    )

  def createIdentifiedItem: Identified[Item] = createIdentifiedItemWith()

  def createIdentifiedItems(count: Int): List[Identified[Item]] =
    (1 to count).map { _ =>
      createIdentifiedItem
    }.toList

  def createIdentifiableItemWith(
    sourceIdentifier: SourceIdentifier = createSourceIdentifier,
    locations: List[Location] = List(defaultLocation)
  ): Identifiable[Item] =
    Identifiable(
      sourceIdentifier = sourceIdentifier,
      agent = Item(locations = locations)
    )

  def createUnidentifiableItemWith(
    locations: List[Location] = List(defaultLocation)) =
    Unidentifiable(
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

  def createImageLocationType = LocationType("iiif-image")

  def createPresentationLocationType = LocationType("iiif-presentation")

  def createStoresLocationType = LocationType("sgmed")

  def createPhysicalItem: Identifiable[Item] =
    createIdentifiableItemWith(locations = List(createPhysicalLocation))

  def createDigitalItem: Unidentifiable[Item] =
    createUnidentifiableItemWith(locations = List(createDigitalLocation))

  private def defaultLocation = createDigitalLocationWith()

  private def defaultLocationUrl =
    "https://iiif.wellcomecollection.org/image/M0000001.jpg/info.json"
}
