package uk.ac.wellcome.models.work.test.util

import uk.ac.wellcome.models.work.internal._

import scala.util.Random

trait WorksUtil extends ItemsUtil {
  val canonicalId = "1234"
  val title = "this is the first image title"
  val description = "this is a description"
  val lettering = "some lettering"
  val period = Period("the past")
  val agent = Agent("a person")
  val workType = WorkType(
    id = "1dz4yn34va",
    label = "An aggregation of angry archipelago aged ankylosaurs."
  )
  val subject = Subject[Unidentifiable[AbstractConcept]](
    label = "a subject created by WorksUtil",
    concepts = List(
      Unidentifiable(Concept("a subject concept")),
      Unidentifiable(Place("a subject place")),
      Unidentifiable(Period("a subject period")))
  )

  val genre = Genre[Unidentifiable[AbstractConcept]](
    label = "an unidentified genre created by WorksUtil",
    concepts = List(
      Unidentifiable(Concept("a genre concept")),
      Unidentifiable(Place("a genre place")),
      Unidentifiable(Period("a genre period")))
  )

  def randomAlphanumeric(length: Int) =
    (Random.alphanumeric take length mkString) toLowerCase

  private def createCanonicalId = randomAlphanumeric(10)

  def createSourceIdentifier: SourceIdentifier = SourceIdentifier(
    identifierType = IdentifierType("miro-image-number"),
    value = randomAlphanumeric(10),
    ontologyType = "Work"
  )

  private def createTitle = randomAlphanumeric(100)

  val sourceIdentifier = SourceIdentifier(
    identifierType = IdentifierType("miro-image-number"),
    "Work",
    "sourceIdentifierFromWorksUtil"
  )

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
    contributors: List[Contributor[MaybeDisplayable[AbstractAgent]]] = List(),
    items: List[Identifiable[Item]] = List()
  ): UnidentifiedWork =
    UnidentifiedWork(
      sourceIdentifier = sourceIdentifier,
      version = version,
      title = createTitle,
      contributors = contributors,
      items = items
    )

  def createUnidentifiedWork: UnidentifiedWork = createUnidentifiedWorkWith()

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
      items = items,
      version = version
    )

  def createIdentifiedWork: IdentifiedWork = createIdentifiedWorkWith()

  def createIdentifiedWorks(count: Int): Seq[IdentifiedWork] =
    (1 to count).map { _ =>
      createIdentifiedWork
    }
}
