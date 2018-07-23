package uk.ac.wellcome.models.work.test.util

import uk.ac.wellcome.models.work.internal._

trait WorksUtil extends ItemsUtil {
  private def createTitle: String = randomAlphanumeric(length = 100)

  def createUnidentifiedRedirectedWork: UnidentifiedRedirectedWork =
    UnidentifiedRedirectedWork(
      sourceIdentifier = createSourceIdentifier,
      version = 1,
      redirect = IdentifiableRedirect(
        sourceIdentifier = createSourceIdentifier
      )
    )

  def createUnidentifiedRedirectedWorkWith(redirect: IdentifiableRedirect): UnidentifiedRedirectedWork =
    UnidentifiedRedirectedWork(
      sourceIdentifier = createSourceIdentifier,
      version = 1,
      redirect = redirect
    )

  def createIdentifiedRedirectedWork: IdentifiedRedirectedWork =
    createIdentifiedRedirectedWorkWith()

  def createIdentifiedRedirectedWorkWith(
    sourceIdentifier: SourceIdentifier = createSourceIdentifier
  ): IdentifiedRedirectedWork =
    IdentifiedRedirectedWork(
      canonicalId = createCanonicalId,
      sourceIdentifier = sourceIdentifier,
      version = 1,
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
    sourceIdentifier: SourceIdentifier = createSourceIdentifier
  ): IdentifiedInvisibleWork =
    IdentifiedInvisibleWork(
      sourceIdentifier = sourceIdentifier,
      version = 1,
      canonicalId = createCanonicalId
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
    contributors: List[Contributor[MaybeDisplayable[AbstractAgent]]] = List(),
    production: List[ProductionEvent[MaybeDisplayable[AbstractAgent]]] = List(),
    items: List[Identifiable[Item]] = List()
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
      thumbnail = None,
      production = production,
      language = None,
      dimensions = None,
      items = items,
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
    subjects: List[Subject[Displayable[AbstractConcept]]] = List(),
    genres: List[Genre[Displayable[AbstractConcept]]] = List(),
    contributors: List[Contributor[Displayable[AbstractAgent]]] = List(),
    thumbnail: Option[Location] = None,
    production: List[ProductionEvent[Displayable[AbstractAgent]]] = List(),
    language: Option[Language] = None,
    items: List[Identified[Item]] = List(),
    version: Int = 1
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
      version = version
    )

  def createIdentifiedWork: IdentifiedWork = createIdentifiedWorkWith()

  def createIdentifiedWorks(count: Int): Seq[IdentifiedWork] =
    (1 to count).map { _ =>
      createIdentifiedWork
    }
}
