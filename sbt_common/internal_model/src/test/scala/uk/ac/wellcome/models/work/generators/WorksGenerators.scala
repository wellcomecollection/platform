package uk.ac.wellcome.models.work.generators

import uk.ac.wellcome.models.work.internal._

trait WorksGenerators extends ItemsGenerators {
  private def createTitle: String = randomAlphanumeric(length = 100)

  def createUnidentifiedRedirectedWork: UnidentifiedRedirectedWork =
    UnidentifiedRedirectedWork(
      sourceIdentifier = createSourceIdentifier,
      version = 1,
      redirect = IdentifiableRedirect(
        sourceIdentifier = createSourceIdentifier
      )
    )

  def createUnidentifiedRedirectedWorkWith(
    redirect: IdentifiableRedirect): UnidentifiedRedirectedWork =
    UnidentifiedRedirectedWork(
      sourceIdentifier = createSourceIdentifier,
      version = 1,
      redirect = redirect
    )

  def createIdentifiedRedirectedWork: IdentifiedRedirectedWork =
    createIdentifiedRedirectedWorkWith()

  def createIdentifiedRedirectedWorkWith(
    canonicalId: String = createCanonicalId,
    sourceIdentifier: SourceIdentifier = createSourceIdentifier,
    version: Int = 1,
  ): IdentifiedRedirectedWork =
    IdentifiedRedirectedWork(
      canonicalId = canonicalId,
      sourceIdentifier = sourceIdentifier,
      version = version,
      redirect = IdentifiedRedirect(
        canonicalId = createCanonicalId
      )
    )

  def createUnidentifiedInvisibleWorkWith(
    sourceIdentifier: SourceIdentifier = createSourceIdentifier
  ): UnidentifiedInvisibleWork =
    UnidentifiedInvisibleWork(
      sourceIdentifier = sourceIdentifier,
      version = 1
    )

  def createUnidentifiedInvisibleWork: UnidentifiedInvisibleWork =
    createUnidentifiedInvisibleWorkWith()

  def createIdentifiedInvisibleWorkWith(
    canonicalId: String = createCanonicalId,
    sourceIdentifier: SourceIdentifier = createSourceIdentifier,
    version: Int = 1
  ): IdentifiedInvisibleWork =
    IdentifiedInvisibleWork(
      sourceIdentifier = sourceIdentifier,
      version = version,
      canonicalId = canonicalId
    )

  def createIdentifiedInvisibleWork: IdentifiedInvisibleWork =
    createIdentifiedInvisibleWorkWith()

  def createIdentifiedInvisibleWorks(count: Int): Seq[IdentifiedInvisibleWork] =
    (1 to count).map { _ =>
      createIdentifiedInvisibleWork
    }

  def createUnidentifiedWorkWith(
    sourceIdentifier: SourceIdentifier = createSourceIdentifier,
    version: Int = 1,
    title: String = createTitle,
    otherIdentifiers: List[SourceIdentifier] = List(),
    mergeCandidates: List[MergeCandidate] = List(),
    description: Option[String] = None,
    lettering: Option[String] = None,
    workType: Option[WorkType] = None,
    thumbnail: Option[Location] = None,
    contributors: List[Contributor[MaybeDisplayable[AbstractAgent]]] = List(),
    production: List[ProductionEvent[MaybeDisplayable[AbstractAgent]]] = List(),
    items: List[MaybeDisplayable[Item]] = List(),
    itemsV1: List[Identifiable[Item]] = List(),
  ): UnidentifiedWork =
    UnidentifiedWork(
      sourceIdentifier = sourceIdentifier,
      otherIdentifiers = otherIdentifiers,
      mergeCandidates = mergeCandidates,
      title = title,
      workType = workType,
      description = description,
      physicalDescription = None,
      extent = None,
      lettering = lettering,
      createdDate = None,
      subjects = List(),
      genres = List(),
      contributors = contributors,
      thumbnail = thumbnail,
      production = production,
      language = None,
      dimensions = None,
      items = items,
      itemsV1 = itemsV1,
      version = version
    )

  def createUnidentifiedWork: UnidentifiedWork = createUnidentifiedWorkWith()

  def createUnidentifiedWorks(count: Int): Seq[UnidentifiedWork] =
    (1 to count).map { _ =>
      createUnidentifiedWork
    }

  def createIdentifiedWorkWith(
    canonicalId: String = createCanonicalId,
    sourceIdentifier: SourceIdentifier = createSourceIdentifier,
    otherIdentifiers: List[SourceIdentifier] = List(),
    title: String = createTitle,
    workType: Option[WorkType] = None,
    description: Option[String] = None,
    physicalDescription: Option[String] = None,
    extent: Option[String] = None,
    lettering: Option[String] = None,
    createdDate: Option[Period] = None,
    subjects: List[Displayable[Subject[Displayable[AbstractRootConcept]]]] =
      List(),
    genres: List[Genre[Displayable[AbstractConcept]]] = List(),
    contributors: List[Contributor[Displayable[AbstractAgent]]] = List(),
    thumbnail: Option[Location] = None,
    production: List[ProductionEvent[Displayable[AbstractAgent]]] = List(),
    language: Option[Language] = None,
    items: List[Displayable[Item]] = List(),
    itemsV1: List[Identified[Item]] = List(),
    version: Int = 1,
    merged: Boolean = false
  ): IdentifiedWork =
    IdentifiedWork(
      canonicalId = canonicalId,
      sourceIdentifier = sourceIdentifier,
      otherIdentifiers = otherIdentifiers,
      mergeCandidates = List(),
      title = title,
      workType = workType,
      description = description,
      physicalDescription = physicalDescription,
      extent = extent,
      lettering = lettering,
      createdDate = createdDate,
      subjects = subjects,
      genres = genres,
      contributors = contributors,
      thumbnail = thumbnail,
      production = production,
      language = language,
      dimensions = None,
      items = items,
      itemsV1 = itemsV1,
      version = version,
      merged = merged
    )

  def createIdentifiedWork: IdentifiedWork = createIdentifiedWorkWith()

  def createIdentifiedWorks(count: Int): Seq[IdentifiedWork] =
    (1 to count).map { _ =>
      createIdentifiedWork
    }

  def createUnidentifiedSierraWorkWith(workType: Option[WorkType] = None,
                                       items: List[MaybeDisplayable[Item]] =
                                         List()): UnidentifiedWork =
    createUnidentifiedWorkWith(
      sourceIdentifier = createSierraSystemSourceIdentifier,
      workType = workType,
      otherIdentifiers = List(createSierraSystemSourceIdentifier),
      items = items
    )

  def createUnidentifiedSierraWork: UnidentifiedWork =
    createUnidentifiedSierraWorkWith()

  def createSierraPhysicalWork: UnidentifiedWork =
    createUnidentifiedSierraWorkWith(
      items = List(
        createIdentifiableItemWith(locations = List(createPhysicalLocation))))

  def createSierraDigitalWork: UnidentifiedWork =
    createUnidentifiedSierraWorkWith(
      workType = Some(WorkType("v", "E-books")),
      items = List(
        createUnidentifiableItemWith(locations = List(createDigitalLocation)))
    )

  def createMiroWorkWith(
    otherIdentifiers: List[SourceIdentifier] = List()): UnidentifiedWork =
    createUnidentifiedWorkWith(
      sourceIdentifier = createMiroSourceIdentifier,
      otherIdentifiers = otherIdentifiers,
      thumbnail = Some(
        DigitalLocation(
          url = "https://iiif.wellcomecollection.org/V01234.jpg",
          locationType = LocationType("thumbnail-image"),
          license = Some(License_CCBY)
        )),
      items = List(
        createUnidentifiableItemWith(locations = List(
          createDigitalLocationWith(locationType = createImageLocationType))))
    )

  def createMiroWork: UnidentifiedWork =
    createMiroWorkWith()

  def createIsbnWork: UnidentifiedWork =
    createUnidentifiedWorkWith(
      sourceIdentifier = createIsbnSourceIdentifier,
    )

  def createIsbnWorks(count: Int): List[UnidentifiedWork] =
    List.fill(count)(createIsbnWork)
}
